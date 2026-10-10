export const API_URLS = {
  // Spring Boot Backend Base URL
  BASE_URL: 'http://localhost:8080',

  // OAuth / Auth Endpointleri
  AUTH: {
    CLICKUP_LOGIN: 'http://localhost:8080/login', // Backend'deki redirectToClickUp tetikleyicisi
  },

  // Analitik Endpointleri (AnalyticsController)
  ANALYTICS: {
    TASK: (taskId: string) => `/api/analytics/task/${taskId}`,
    SPRINT: (listId: string) => `/api/analytics/sprint/${listId}`,
    USER_GANTT: (userId: number | string) => `/api/analytics/user/${userId}/gantt`,
  },
};