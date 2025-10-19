/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'asana-blue': '#4A90E2',
        'asana-green': '#7AC142',
        'asana-red': '#E24A4A',
        'asana-orange': '#F2994A',
        'asana-purple': '#9B51E0',
      }
    },
  },
  plugins: [],
}
