export interface TaskAnalyticsResponse {
    taskId:string;
    taskName:string;
    currentStatus:string | null;
    priority:string | null;
    startDate:string | null;
    endDate:string | null;
    totalActiveWorkingTime:string;
    columnDetails:Record<string, ColumnDetail>;
    columnHistory:ColumnVisit[];
    userActiveWorkingTimes:Record<string, string>;
    rollbackCount:number;
    stalled:boolean;
    blockedByTaskId:string | null;
    scopeCreep:boolean;

}
export interface ColumnDetail {
    statusType:string;
    totalDurationFormatted:string;
    assigneeTimes:Record<string, string>;
}
export interface ColumnVisit extends ColumnDetail {
    statusName:string;
    visitNumber:number;
}

export interface TaskSummaryDto {
    taskI:string;
    taskName:string;
    currentStatus:string;
    
}

export interface SprintAnalyticsResponse { 
    ListId:string;
    Listname:string;
    EffectiveSprintStartDate:number | null;
    SpringDueDate:number | null ;

    totalTasks:number;
    completedTasks:number;
    inProgressTasks:number;
    todoTasks:number;
    completionRatio:string;
    totalActiveAssignees:number;

    scopeCreepTaskCount:number;
    stalledTaskCount:TaskSummaryDto[];

    totalRollbackCount:number;

    totalSprintActiveWorkingTime:string;
    bottleneckColumn:string | null;
    userTotalActiveWorkingTimes:Record<string, string>;

    taskAnalyticsList:TaskAnalyticsResponse[];

}
export interface WorkIntervalDto {
    statusName:string;
    startTimestamp:number;
    endTimestamp:number;
    durationFormatted:string;
    durationMillis:number;
}
export interface UserTaskGanttDto {
    taskId:string;
    taskName:string;
    currentStatus:string;
    totalUserDurationOnTask:string;
    firstStartedAt:number;
    lastEndedAt:number;
    intervals:WorkIntervalDto[];}

export interface UserGanttAnalyticsResponse {
    userId:number;
    listId:string | null;
    netTotalActiveWorkingTime:string;
    netTotalActiveMillis:number;
    rawTotalTaskWorkingTime:string;
    totalAssignedTasksCount:number;
    taskTimelines:UserTaskGanttDto[];}

export interface AvailableColumn {
  id: string;
  status: string;
  orderindex: number;
  color: string;
  type: string;
}

export interface AnalyticsUser {
  id: number;
  username: string;
  email: string;
  profile_picture: string | null;
  date_joined: string | null;
  date_invited: string | null;
  role_key: string | null;
}

export interface ScopeCreepTask {
  taskId: string;
  taskName: string;
  currentStatus: string;
}

export interface AnalyticsData {
  availableColumns: AvailableColumn[];
  usersMap: Record<string, AnalyticsUser>;
  totalTasks: number;
  completedTasks: number;
  completionRatio: string;
  totalActiveAssignees: number;
  taskCountPerColumn: Record<string, number>;
  userTaskCounts: Record<string, number>;
  scopeCreepTaskCount: number;
  scopeCreepTasks: ScopeCreepTask[];
}