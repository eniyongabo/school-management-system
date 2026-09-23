import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests",
  workers: 1,
  timeout: 30000,
  use: {
    baseURL: "http://127.0.0.1:15173",
    headless: true,
    viewport: { width: 1440, height: 1000 },
    launchOptions: process.env.PLAYWRIGHT_CHROME_PATH
      ? { executablePath: process.env.PLAYWRIGHT_CHROME_PATH }
      : {},
  },
  webServer: [
    {
      command: "sh ../scripts/browser-api.sh",
      url: "http://127.0.0.1:18080/actuator/health",
      timeout: 120000,
      reuseExistingServer: false,
    },
    {
      command: "npm run dev -- --port 15173",
      url: "http://127.0.0.1:15173",
      reuseExistingServer: false,
      env: { VITE_PROXY_TARGET: "http://127.0.0.1:18080" },
    },
  ],
  reporter: "list",
});
