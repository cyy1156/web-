/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#EFF6FF',
          100: '#DBEAFE',
          200: '#BFDBFE',
          300: '#93C5FD',
          400: '#60A5FA',
          500: '#3B82F6',
          600: '#2563EB',
          700: '#1D4ED8',
          800: '#1E40AF',
          900: '#1E3A8A'
        },
        ink: '#111827',
        sub: '#6B7280',
        line: '#E5E7EB',
        canvas: '#F5F7FA',
        navy: '#0F1B33'
      },
      borderRadius: {
        card: '12px'
      },
      boxShadow: {
        card: '0 1px 2px rgba(16,24,40,.06), 0 1px 3px rgba(16,24,40,.10)',
        hover: '0 8px 24px rgba(37,99,235,.12)',
        float: '0 12px 32px rgba(15,27,51,.12)'
      },
      fontFamily: {
        sans: [
          'PingFang SC',
          'HarmonyOS Sans SC',
          'Microsoft YaHei',
          'system-ui',
          '-apple-system',
          'sans-serif'
        ]
      },
      keyframes: {
        'fade-up': {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' }
        },
        float: {
          '0%,100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-12px)' }
        },
        shimmer: {
          '0%': { backgroundPosition: '-400px 0' },
          '100%': { backgroundPosition: '400px 0' }
        }
      },
      animation: {
        'fade-up': 'fade-up .28s ease-out both',
        float: 'float 7s ease-in-out infinite',
        shimmer: 'shimmer 1.4s linear infinite'
      }
    }
  },
  plugins: [require('tailwindcss-animate')]
}
