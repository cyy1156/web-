#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
抢课并发测试：验证任何并发下都不会超卖。

用法：
    python scripts/concurrent_test.py --class-id 101 --threads 50

原理：
    1. 从数据库取若干学生账号（默认跳过演示账号）
    2. 并发登录拿到 token
    3. 用栅栏让所有请求同时发出，争抢同一个教学班
    4. 统计成功数与 40002（已满）数，并做一致性对账

依赖：本机 mysql 客户端（用于取账号与对账），HTTP 用标准库 urllib。
"""

import argparse
import json
import random
import re
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api"
LOCK = threading.Lock()

# Windows 控制台默认 GBK，统一用 UTF-8 输出，避免中文与符号乱码
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")


def mysql_query(sql: str, database: str, user: str, password: str) -> list[list[str]]:
    cmd = ["mysql", f"-u{user}", "-N", "-B", "-e", sql, database]
    if password:
        cmd.insert(1, f"-p{password}")
    out = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")
    if out.returncode != 0:
        raise RuntimeError(out.stderr.strip() or "mysql 执行失败")
    return [line.split("\t") for line in out.stdout.strip().splitlines() if line]


def post(path: str, payload: dict, token: str | None = None, idem: str | None = None) -> dict:
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(BASE + path, data=data, method="POST")
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    if idem:
        req.add_header("Idempotency-Key", idem)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--class-id", type=int, required=True, help="要抢的教学班 id")
    ap.add_argument("--threads", type=int, default=50, help="并发学生数")
    ap.add_argument("--db", default="course_selection")
    ap.add_argument("--user", default="root")
    ap.add_argument("--password", default="")
    ap.add_argument("--skip-demo", action="store_true", default=True)
    args = ap.parse_args()

    rows = mysql_query(
        f"SELECT student_no FROM student WHERE id <> 1 ORDER BY RAND() LIMIT {args.threads}",
        args.db, args.user, args.password)
    usernames = [r[0] for r in rows]
    if not usernames:
        raise SystemExit("没有取到学生账号，请检查数据库配置")

    print(f"[1/4] 取到 {len(usernames)} 个学生账号")

    tokens: list[str] = []

    def login(name: str):
        res = post("/auth/login", {"username": name, "password": "123456"})
        if res.get("code") == 0:
            with LOCK:
                tokens.append(res["data"]["token"])

    threads = [threading.Thread(target=login, args=(u,)) for u in usernames]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    print(f"[2/4] 登录成功 {len(tokens)} 个")

    before = mysql_query(
        f"SELECT capacity, selected_count FROM teaching_class WHERE id = {args.class_id}",
        args.db, args.user, args.password)[0]
    capacity_before, selected_before = int(before[0]), int(before[1])
    remaining_before = max(0, capacity_before - selected_before)
    expected = min(len(tokens), remaining_before)
    print(f"      抢课前容量 {capacity_before}，已选 {selected_before}，剩余 {remaining_before}")

    barrier = threading.Barrier(len(tokens))
    results = {"ok": 0, "full": 0, "other": 0, "codes": {}}

    def grab(token: str):
        barrier.wait()
        idem = f"load-{random.randint(1, 10 ** 12)}"
        res = post("/enrollments", {"teachingClassId": args.class_id}, token, idem)
        code = res.get("code")
        with LOCK:
            results["codes"][code] = results["codes"].get(code, 0) + 1
            if code == 0:
                results["ok"] += 1
            elif code == 40002:
                results["full"] += 1
            else:
                results["other"] += 1
                print("  其他结果:", code, res.get("message"))

    print(f"[3/4] {len(tokens)} 个请求同时发出 …")
    started = time.time()
    threads = [threading.Thread(target=grab, args=(t,)) for t in tokens]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    cost = time.time() - started

    stat = mysql_query(
        f"SELECT capacity, selected_count FROM teaching_class WHERE id = {args.class_id}",
        args.db, args.user, args.password)[0]
    actual = mysql_query(
        f"SELECT COUNT(*) FROM enrollment WHERE teaching_class_id = {args.class_id} AND status = 'SELECTED'",
        args.db, args.user, args.password)[0][0]

    capacity, selected = int(stat[0]), int(stat[1])
    actual = int(actual)
    print(f"[4/4] 耗时 {cost:.2f}s")
    print("---------------------------------------------")
    print(f"容量            : {capacity}")
    print(f"成功选课        : {results['ok']}")
    print(f"返回 40002      : {results['full']}")
    print(f"其他失败        : {results['other']}")
    print(f"selected_count  : {selected}")
    print(f"实际记录数      : {actual}")
    print("---------------------------------------------")
    print(f"选课前剩余      : {remaining_before}")
    ok = selected == actual and selected <= capacity and results["ok"] == (selected - selected_before)
    print("结论            :", "[PASS] 无超卖，数据一致" if ok else "[FAIL] 校验失败，请检查事务与扣减逻辑")
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
