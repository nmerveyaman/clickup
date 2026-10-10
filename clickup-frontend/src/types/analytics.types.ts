export interface TaskAnalyticsResponse {
    taskId:string;
    taskName:string;
    startDate:string | null;
    endDate:string | null;
    totalActiveWorkingTime:string;
    columnDetails:Record<string, ColumnDetail>;
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