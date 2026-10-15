export interface ClickUpListDto {
  id: string;
  name: string;
  taskCount?: number;
  spaceId?: string;
  folderId?: string;
}

export interface ClickUpListResponseDto {
  lists: ClickUpListDto[];
}

export interface TaskSummaryDto {
  id: string;
  name: string;
  statusName: string;
}

export interface SprintAnalyticsResponse {
  listId: string;
  listName: string;
  effectiveSprintStartDate?: number;
  sprintDueDate?: number;
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  todoTasks: number;
  completionRatio: string;
  totalActiveAssignees: number;
  scopeCreepTaskCount: number;
  scopeCreepTasks: TaskSummaryDto[];
  stalledTaskCount: number;
  stalledTasks: TaskSummaryDto[];
  blockedTaskCount: number;
  blockedTasks: TaskSummaryDto[];
  totalRollbackCount: number;
  totalSprintActiveWorkingTime: string;
  bottleneckColumn?: string;
  userTotalActiveWorkingTimes: Record<string, string>;
  taskAnalyticsList: any[];
}

export interface UserGanttAnalyticsResponse {
  userId: number;
  listId?: string;
  netTotalActiveWorkingTime: string;
  rawTotalTaskWorkingTime: string;
  totalAssignedTasksCount: number;
  taskTimelines: {
    taskId: string;
    taskName: string;
    statusName: string;
    totalWorkingTime: string;
    firstStartedAt: number;
    lastEndedAt: number;
    workIntervals: {
      statusName: string;
      startTimestamp: number;
      endTimestamp: number;
      durationFormatted: string;
      durationMillis: number;
    }[];
  }[];
}