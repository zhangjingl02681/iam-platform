import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  base: "/",
  server: {
    port: 5173,
    proxy: {
      "/api": { target: "http://localhost:8080", changeOrigin: true },
      "/logout": { target: "http://localhost:8080", changeOrigin: true },
      "/oauth2": { target: "http://localhost:8080", changeOrigin: true },
      "/userinfo": { target: "http://localhost:8080", changeOrigin: true },
      "/connect": { target: "http://localhost:8080", changeOrigin: true },
      "/csrf": { target: "http://localhost:8080", changeOrigin: true },
      "/.well-known": { target: "http://localhost:8080", changeOrigin: true },
      "/actuator": { target: "http://localhost:8080", changeOrigin: true },
      "/login": {
        target: "http://localhost:8080",
        changeOrigin: true,
        bypass(req) {
          return req.method === "GET" ? req.url : undefined;
        },
      },
    },
  },
  build: {
    outDir: "dist",
    emptyOutDir: true,
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./src/test/setup.ts",
    css: true,
  },
});
