export const BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

export const API_URLS = {
  // Auth
  LOGIN: `${BASE_URL}/login`,

  // Workspace / Sprint Listesi
  WORKSPACES: {
    GET_LISTS: (workspaceId: string) => `${BASE_URL}/api/v1/workspaces/lists?workspaceId=${workspaceId}`,
  },

  // Analitikler
  ANALYTICS: {
    SPRINT_DETAILS: (listId: string) => `${BASE_URL}/api/analytics/sprint/${listId}`,
    TASK_DETAILS: (taskId: string) => `${BASE_URL}/api/analytics/task/${taskId}`,
    USER_GANTT: (userId: number | string, listId?: string) =>
      `${BASE_URL}/api/analytics/user/${userId}/gantt${listId ? `?listId=${listId}` : ''}`,
  },
};