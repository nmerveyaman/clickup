// import axios from 'axios';
// import type {
//   SprintAnalyticsResponse,
//   TaskAnalyticsResponse,
//   UserGanttAnalyticsResponse,
// } from '../types/analytics.types'; // Tiplerinin olduğu dosya yolu
//
// export const analyticsApi = {
//   // 1. Sprint (List) Bazlı Toplu Özet Analizi
//   getSprintAnalytics: async (listId: string): Promise<SprintAnalyticsResponse> => {
//     const response = await axios.get<SprintAnalyticsResponse>(`/api/analytics/sprint/${listId}`);
//     return response.data;
//   },
//
//   // 2. Tekil Görev (Task) Kapsamlı Analizi
//   getTaskAnalytics: async (taskId: string): Promise<TaskAnalyticsResponse> => {
//     const response = await axios.get<TaskAnalyticsResponse>(`/api/analytics/task/${taskId}`);
//     return response.data;
//   },
//
//   // 3. Kullanıcı (Assignee) Bazlı Gantt Çizelgesi
//   getUserGanttAnalytics: async (userId: number | string, listId?: string): Promise<UserGanttAnalyticsResponse> => {
//     const response = await axios.get<UserGanttAnalyticsResponse>(`/api/analytics/user/${userId}/gantt`, {
//       params: listId ? { listId } : undefined,
//     });
//     return response.data;
//   },
// };