// import axios from 'axios';
// import { API_URLS } from '../constants/apiUrls';
// import type {
//   SprintAnalyticsResponse,
//   TaskAnalyticsResponse,
//   UserGanttAnalyticsResponse,
// } from './analytics.types';
//
// const apiClient = axios.create({
//   baseURL: API_URLS.BASE_URL,
//   withCredentials: true, // Eğer cookie/session kullanıyorsan gerekli
// });
//
// /*export const analyticsApi = {
//   getSprintAnalytics: async (listId: string): Promise<SprintAnalyticsResponse> => {
//     const response = await apiClient.get<SprintAnalyticsResponse>(
//       API_URLS.ANALYTICS.SPRINT(listId)
//     );
//     return response.data;
//   },
// */
//   getTaskAnalytics: async (taskId: string): Promise<TaskAnalyticsResponse> => {
//     const response = await apiClient.get<TaskAnalyticsResponse>(
//       API_URLS.ANALYTICS.TASK(taskId)
//     );
//     return response.data;
//   },
//
//   getUserGanttAnalytics: async (
//     userId: number | string,
//     listId?: string
//   ): Promise<UserGanttAnalyticsResponse> => {
//     const response = await apiClient.get<UserGanttAnalyticsResponse>(
//       API_URLS.ANALYTICS.USER_GANTT(userId),
//       {
//         params: listId ? { listId } : undefined,
//       }
//     );
//     return response.data;
//   },
// };
//
