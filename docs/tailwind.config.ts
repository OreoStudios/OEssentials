import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}", "./lib/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        surface: {
          950: "#050b15",
          900: "#08101d",
          850: "#0b1527",
          800: "#0f1b31"
        },
        electric: {
          300: "#79e5ff",
          400: "#32d5ff",
          500: "#0fc3ff",
          600: "#0597d4"
        },
        royal: {
          400: "#3e7bff",
          500: "#2459df",
          600: "#193ca7"
        }
      },
      boxShadow: {
        glow: "0 0 0 1px rgba(122, 229, 255, 0.16), 0 20px 80px rgba(12, 195, 255, 0.12)",
        premium: "0 24px 80px rgba(0, 0, 0, 0.45)"
      },
      backgroundImage: {
        "hero-grid":
          "linear-gradient(rgba(126, 227, 255, 0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(126, 227, 255, 0.08) 1px, transparent 1px)"
      }
    }
  },
  plugins: []
};

export default config;
