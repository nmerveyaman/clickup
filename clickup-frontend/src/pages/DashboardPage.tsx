import React, { useState, useEffect, useMemo, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  PieChart as PieIcon,
  Clock,
  User,
  X,
  Layers,
  Activity,
  Calendar,
  CheckCircle,
  PlayCircle,
  GitCommit,
  LogOut,
  Zap,
  AlertTriangle,
  Users,
  TrendingUp,
  ChevronDown,
  ChevronUp,
  ExternalLink,
  RotateCcw,
  Ban,
  ShieldAlert,
  Bell,
  Trash2
} from 'lucide-react';
import { UserAvatar } from './UserAvatar';
import { BASE_URL } from '../constants/apiUrls';

type TaskNotification = {
  id: string;
  workspaceId: string;
  listId: string | null;
  taskId: string;
  taskName: string;
  eventType: string;
  fromStatus: string | null;
  toStatus: string | null;
  fromPriority: string | null;
  toPriority: string | null;
  timestamp: string;
  read: boolean;
};

const MAX_SAVED_NOTIFICATIONS = 100;

export default function DashboardPage() {
  const navigate = useNavigate();
  const [isSidebarHovered, setIsSidebarHovered] = useState(false);
  const [sprintList, setSprintList] = useState<any[]>([]);
  const [selectedSprintId, setSelectedSprintId] = useState<string | null>(null);
  const [availableWorkspaces, setAvailableWorkspaces] = useState<{ id: string; name: string }[]>([]);
  const [workspaceLoadError, setWorkspaceLoadError] = useState('');
  const [sprintAnalytics, setSprintAnalytics] = useState<any>(null);
  const [selectedTask, setSelectedTask] = useState<any>(null);
  const selectedTaskRef = useRef<any>(null);
  selectedTaskRef.current = selectedTask;
  const [loading, setLoading] = useState(false);
  const [scopeCreepExpanded, setScopeCreepExpanded] = useState(false);
  const [hoveredCol, setHoveredCol] = useState<string | null>(null);
  const donutSlices: any[] = [];

  // Resolve the active workspace and user from the URL or local storage.
  const queryParams = new URLSearchParams(window.location.search);
  const [workspaceId, setWorkspaceId] = useState(
    () => queryParams.get('workspaceId') || localStorage.getItem('workspaceId') || ''
  );
  const userId = queryParams.get('userId') || localStorage.getItem('userId');
  const notificationStorageKey = `clickup-notifications:${workspaceId || 'unknown'}`;
  const selectedSprintIdRef = useRef(selectedSprintId);
  selectedSprintIdRef.current = selectedSprintId;
  const [notifications, setNotifications] = useState<TaskNotification[]>(() => {
    try {
      const saved = localStorage.getItem(notificationStorageKey);
      return saved ? (JSON.parse(saved) as TaskNotification[]).slice(0, MAX_SAVED_NOTIFICATIONS) : [];
    } catch (error) {
      console.error('Failed to load saved notifications:', error);
      return [];
    }
  });
  const [notificationsLoadedFor, setNotificationsLoadedFor] = useState(notificationStorageKey);
  const seenNotificationIds = useRef(new Set(notifications.map((notification) => notification.id)));
  const specificTaskEvents = useRef(new Set<string>());
  const [notificationStorageError, setNotificationStorageError] = useState(false);
  const [notificationPanelOpen, setNotificationPanelOpen] = useState(false);
  const [streamConnected, setStreamConnected] = useState(false);
  const [toastNotification, setToastNotification] = useState<TaskNotification | null>(null);

  // Sign out and clear the local session.
  const handleLogout = () => {
    localStorage.removeItem('userId');
    localStorage.removeItem('workspaceId');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('isLoggedIn');

    navigate('/login', { replace: true });
  };

  // Keep the dashboard behind the login screen when there is no session.
  useEffect(() => {
    if (!workspaceId && !userId) {
      navigate('/login');
    } else {
      if (workspaceId) localStorage.setItem('workspaceId', workspaceId);
      if (userId) localStorage.setItem('userId', userId);
    }
  }, [workspaceId, userId, navigate]);

  useEffect(() => {
    if (!userId) return;

    const fetchWorkspaces = async () => {
      try {
        const response = await fetch(
          `${BASE_URL}/api/v1/workspaces/available?userId=${encodeURIComponent(userId)}`
        );
        if (!response.ok) {
          throw new Error(`Workspace request failed (${response.status})`);
        }
        setAvailableWorkspaces(await response.json());
        setWorkspaceLoadError('');
      } catch (error) {
        console.error('Failed to load workspaces:', error);
        setWorkspaceLoadError('Workspace listesi yüklenemedi.');
      }
    };

    fetchWorkspaces();
  }, [userId]);

  const handleWorkspaceChange = (nextWorkspaceId: string) => {
    setWorkspaceId(nextWorkspaceId);
    localStorage.setItem('workspaceId', nextWorkspaceId);
    const params = new URLSearchParams(window.location.search);
    params.set('workspaceId', nextWorkspaceId);
    window.history.replaceState(null, '', `${window.location.pathname}?${params.toString()}`);
    setSprintList([]);
    setSelectedSprintId(null);
    setSprintAnalytics(null);
  };

  const fetchSprintAnalytics = useCallback(async (listId: string) => {
    setSelectedSprintId(listId);
    setLoading(true);
    try {
      const res = await fetch(`${BASE_URL}/api/analytics/sprint/${listId}`);
      if (res.ok) {
        const data = await res.json();
        setSprintAnalytics(data);
      }
    } catch (err) {
      console.error("Failed to load sprint analytics:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchTaskAnalytics = useCallback(async (taskId: string) => {
    const response = await fetch(`${BASE_URL}/api/analytics/task/${encodeURIComponent(taskId)}`);
    if (!response.ok) {
      throw new Error(`Task analytics request failed (${response.status})`);
    }
    return response.json();
  }, []);

  const openTaskDetails = useCallback(async (task: any) => {
    setSelectedTask(task);
    try {
      const latestTask = await fetchTaskAnalytics(task.taskId);
      setSelectedTask((current: any) => current?.taskId === task.taskId
        ? {
            ...current,
            ...latestTask,
            priority: latestTask.priority ?? current.priority ?? null,
          }
        : current);
    } catch (error) {
      console.error('Failed to refresh task details:', error);
    }
  }, [fetchTaskAnalytics]);

  // Load the workspace sprint list.
  useEffect(() => {
    if (!workspaceId) return;

    const fetchSprintList = async () => {
      try {
        const res = await fetch(`${BASE_URL}/api/v1/workspaces/lists?workspaceId=${encodeURIComponent(workspaceId)}`);
        if (res.ok) {
          const data = await res.json();
          const lists = data.lists || data || [];
          setSprintList(lists);

          if (lists.length > 0) {
            const firstListId = lists[0].id;
            setSelectedSprintId(firstListId);
            fetchSprintAnalytics(firstListId);
          }
        }
      } catch (err) {
        console.error("Failed to load sprint list:", err);
      }
    };

    fetchSprintList();
  }, [workspaceId, fetchSprintAnalytics]);

  useEffect(() => {
    if (!workspaceId) {
      setStreamConnected(false);
      return;
    }

    setStreamConnected(false);
    const eventSource = new EventSource(
      `${BASE_URL}/api/events/stream?workspaceId=${encodeURIComponent(workspaceId)}`
    );

    eventSource.onopen = () => setStreamConnected(true);
    eventSource.onerror = () => setStreamConnected(false);
    eventSource.addEventListener('connected', () => setStreamConnected(true));
    eventSource.addEventListener('task-event', (event: MessageEvent<string>) => {
      try {
        const payload = JSON.parse(event.data);
        if (!payload.taskId || !payload.eventType) {
          console.error('Received an invalid task notification payload:', payload);
          return;
        }

        const timestamp = String(payload.timestamp || Date.now());
        const notification: TaskNotification = {
          id: payload.eventId
            ? String(payload.eventId)
            : `${payload.taskId}:${payload.eventType}:${timestamp}`,
          workspaceId,
          listId: payload.listId ? String(payload.listId) : null,
          taskId: String(payload.taskId),
          taskName: String(payload.taskName || 'Untitled task'),
          eventType: String(payload.eventType),
          fromStatus: payload.fromStatus ? String(payload.fromStatus) : null,
          toStatus: payload.toStatus ? String(payload.toStatus) : null,
          fromPriority: payload.fromPriority ? String(payload.fromPriority) : null,
          toPriority: payload.toPriority ? String(payload.toPriority) : null,
          timestamp,
          read: false,
        };

        const taskUpdateKey = `${notification.taskId}:${notification.timestamp}`;
        const isGenericUpdate = notification.eventType === 'TASK_UPDATED';
        const isSpecificUpdate = !isGenericUpdate && notification.eventType !== 'TASK_CREATED';
        if (isGenericUpdate && specificTaskEvents.current.has(taskUpdateKey)) return;
        if (isSpecificUpdate) {
          specificTaskEvents.current.add(taskUpdateKey);
          if (specificTaskEvents.current.size > 500) {
            const oldestKey = specificTaskEvents.current.values().next().value;
            if (oldestKey) specificTaskEvents.current.delete(oldestKey);
          }
        }
        if (seenNotificationIds.current.has(notification.id)) return;
        seenNotificationIds.current.add(notification.id);
        setNotifications((current) => [
          notification,
          ...current.filter((item) => !(
            (isGenericUpdate && item.eventType === 'TASK_UPDATED'
              || isSpecificUpdate && item.eventType === 'TASK_UPDATED')
            && item.taskId === notification.taskId
            && item.timestamp === notification.timestamp
          )),
        ].slice(0, MAX_SAVED_NOTIFICATIONS));
        setToastNotification(notification);

        if (notification.eventType === 'PRIORITY_CHANGED') {
          setSprintAnalytics((current: any) => current ? {
            ...current,
            taskAnalyticsList: current.taskAnalyticsList?.map((task: any) =>
              task.taskId === notification.taskId
                ? { ...task, priority: notification.toPriority }
                : task
            ),
          } : current);
          setSelectedTask((current: any) =>
            current?.taskId === notification.taskId
              ? { ...current, priority: notification.toPriority }
              : current
          );
        }

        if (notification.listId && notification.listId === selectedSprintIdRef.current) {
          void fetchSprintAnalytics(notification.listId);
        }
        if (selectedTaskRef.current?.taskId === notification.taskId) {
          void fetchTaskAnalytics(notification.taskId)
            .then((latestTask: any) => {
              setSelectedTask((current: any) => current?.taskId === notification.taskId
                ? {
                    ...current,
                    ...latestTask,
                    priority: latestTask.priority ?? (
                      notification.eventType === 'PRIORITY_CHANGED'
                        ? notification.toPriority
                        : current.priority
                    ),
                  }
                : current);
            })
            .catch((error: unknown) => console.error('Failed to refresh task details after notification:', error));
        }
      } catch (error) {
        console.error('Failed to process task notification:', error);
      }
    });

    return () => {
      eventSource.close();
      setStreamConnected(false);
    };
  }, [workspaceId, fetchSprintAnalytics, fetchTaskAnalytics]);

  useEffect(() => {
    try {
      const saved = localStorage.getItem(notificationStorageKey);
      const restored = saved ? (JSON.parse(saved) as TaskNotification[]).slice(0, MAX_SAVED_NOTIFICATIONS) : [];
      setNotifications(restored);
      seenNotificationIds.current = new Set(restored.map((notification) => notification.id));
      setNotificationStorageError(false);
      specificTaskEvents.current.clear();
      restored.forEach((notification) => {
        if (notification.eventType !== 'TASK_UPDATED' && notification.eventType !== 'TASK_CREATED') {
          specificTaskEvents.current.add(`${notification.taskId}:${notification.timestamp}`);
        }
      });
    } catch (error) {
      console.error('Failed to load saved notifications:', error);
      setNotifications([]);
      seenNotificationIds.current.clear();
      specificTaskEvents.current.clear();
      setNotificationStorageError(true);
    }
    setNotificationsLoadedFor(notificationStorageKey);
  }, [notificationStorageKey]);

  useEffect(() => {
    if (!workspaceId || notificationsLoadedFor !== notificationStorageKey) return;
    try {
      localStorage.setItem(notificationStorageKey, JSON.stringify(notifications));
      setNotificationStorageError(false);
    } catch (error) {
      console.error('Failed to save notifications:', error);
      setNotificationStorageError(true);
    }
  }, [notificationStorageKey, notifications, notificationsLoadedFor, workspaceId]);

  useEffect(() => {
    if (!toastNotification) return;
    const timeout = window.setTimeout(() => setToastNotification(null), 5000);
    return () => window.clearTimeout(timeout);
  }, [toastNotification]);

  const unreadNotificationCount = notifications.filter((notification) => !notification.read).length;
  const markNotificationRead = (notificationId: string) => {
    setNotifications((current) =>
      current.map((notification) => notification.id === notificationId
        ? { ...notification, read: true }
        : notification)
    );
  };

  const getNotificationMessage = (notification: TaskNotification) => {
    switch (notification.eventType) {
      case 'TASK_CREATED':
        return 'Task added to the sprint';
      case 'TASK_UPDATED':
        return 'Task details updated';
      case 'TASK_COMPLETED':
        return 'Task completed';
      case 'STATUS_PROGRESSED':
        return `Status changed${notification.fromStatus && notification.toStatus
          ? ` from ${formatStatusName(notification.fromStatus)} to ${formatStatusName(notification.toStatus)}`
          : ''}`;
      case 'STATUS_ROLLBACK':
        return `Status moved back${notification.fromStatus && notification.toStatus
          ? ` from ${formatStatusName(notification.fromStatus)} to ${formatStatusName(notification.toStatus)}`
          : ''}`;
      case 'ASSIGNEE_CHANGED':
        return 'Assignee changed';
      case 'PRIORITY_CHANGED':
        return `Priority changed${notification.fromPriority || notification.toPriority
          ? ` from ${notification.fromPriority || 'none'} to ${notification.toPriority || 'none'}`
          : ''}`;
      case 'MOVED_TO_LIST':
        return 'Task moved to another list';
      default:
        return notification.eventType.replace(/_/g, ' ').toLowerCase();
    }
  };

  const formatNotificationTime = (timestamp: string) => {
    const numericTimestamp = Number(timestamp);
    const date = new Date(Number.isFinite(numericTimestamp) ? numericTimestamp : timestamp);
    return Number.isNaN(date.getTime())
      ? 'Just now'
      : date.toLocaleString('en-US', { dateStyle: 'short', timeStyle: 'short' });
  };

  // Format a date for display.
  const formatDate = (timestamp?: number | string | null) => {
    if (!timestamp) return null;
    const num = typeof timestamp === 'string' ? Number(timestamp) : timestamp;
    if (!num || isNaN(num)) return null;
    return new Date(num).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' });
  };

  // Format a date and time for display.
  const formatDateTime = (timestamp?: number | string | null) => {
    if (!timestamp) return null;
    const num = typeof timestamp === 'string' ? Number(timestamp) : timestamp;
    if (!num || isNaN(num)) return null;
    return new Date(num).toLocaleString('en-US', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatDuration = (duration?: string | null) => {
    if (!duration) return '0 seconds';

    return duration.replace(/(\d+)\s*(Gün|Saat|Dakika|Saniye)/gi, (_, amount: string, unit: string) => {
      const unitNames: Record<string, [string, string]> = {
        gün: ['day', 'days'],
        saat: ['hour', 'hours'],
        dakika: ['minute', 'minutes'],
        saniye: ['second', 'seconds'],
      };
      const count = Number(amount);
      const [singular, plural] = unitNames[unit.toLocaleLowerCase('tr-TR')] || ['second', 'seconds'];
      return `${count} ${count === 1 ? singular : plural}`;
    });
  };

  // Map status types to timeline badge styles.
  const getStatusTypeConfig = (statusType: string) => {
    const s = (statusType || '').toLowerCase().trim();
    if (s === 'unstarted' || s === 'to do' || s === 'todo') {
      return {
        label: 'Not started',
        dotColor: 'bg-slate-400 ring-slate-500/30',
        badgeBg: 'bg-slate-500/15',
        badgeText: 'text-slate-300',
        badgeBorder: 'border-slate-500/30',
        borderAccent: 'border-slate-700/60',
        titleColor: 'text-slate-200',
        hex: '#94a3b8'
      };
    }
    if (s === 'custom') {
      return {
        label: 'Custom',
        dotColor: 'bg-amber-400 ring-amber-500/30',
        badgeBg: 'bg-amber-500/15',
        badgeText: 'text-amber-300',
        badgeBorder: 'border-amber-500/30',
        borderAccent: 'border-amber-500/30',
        titleColor: 'text-amber-300',
        hex: '#f59e0b'
      };
    }
    if (s === 'in progress' || s === 'in_progress' || s === 'active') {
      return {
        label: 'In progress',
        dotColor: 'bg-blue-400 ring-blue-500/30',
        badgeBg: 'bg-blue-500/15',
        badgeText: 'text-blue-300',
        badgeBorder: 'border-blue-500/30',
        borderAccent: 'border-blue-500/30',
        titleColor: 'text-blue-300',
        hex: '#3b82f6'
      };
    }
    if (s === 'complete' || s === 'completed' || s === 'closed' || s === 'done') {
      return {
        label: 'Complete',
        dotColor: 'bg-emerald-400 ring-emerald-500/30',
        badgeBg: 'bg-emerald-500/15',
        badgeText: 'text-emerald-300',
        badgeBorder: 'border-emerald-500/30',
        borderAccent: 'border-emerald-500/30',
        titleColor: 'text-emerald-300',
        hex: '#10b981'
      };
    }
    if (s === 'review' || s === 'in review' || s === 'in_review' || s === 'qa') {
      return {
        label: 'In review',
        dotColor: 'bg-purple-400 ring-purple-500/30',
        badgeBg: 'bg-purple-500/15',
        badgeText: 'text-purple-300',
        badgeBorder: 'border-purple-500/30',
        borderAccent: 'border-purple-500/30',
        titleColor: 'text-purple-300',
        hex: '#a855f7'
      };
    }
    return {
      label: statusType ? 'Other' : 'Custom',
      dotColor: 'bg-indigo-400 ring-indigo-500/30',
      badgeBg: 'bg-indigo-500/15',
      badgeText: 'text-indigo-300',
      badgeBorder: 'border-indigo-500/30',
      borderAccent: 'border-indigo-500/30',
      titleColor: 'text-indigo-300',
      hex: '#6366f1'
    };
  };

  const startDateStr = formatDate(sprintAnalytics?.effectiveSprintStartDate);
  const dueDateStr = formatDate(sprintAnalytics?.sprintDueDate);

  const isSprintFinished = sprintAnalytics?.sprintDueDate
    ? Date.now() > sprintAnalytics.sprintDueDate
    : (sprintAnalytics?.completedTasks > 0 && sprintAnalytics?.completedTasks === sprintAnalytics?.totalTasks);

  // Normalize status names for comparisons.
  const normalizeStatus = (str: string): string => {
    const clean = (str || '').toLowerCase().replace(/[_\s-]+/g, ' ').trim();
    if (clean === 'todo') return 'to do';
    return clean;
  };

  const formatStatusName = (status: string): string => {
    const normalized = normalizeStatus(status);
    const standardStatuses: Record<string, string> = {
      'to do': 'To do',
      'in progress': 'In progress',
      'in review': 'In review',
      complete: 'Complete',
      completed: 'Complete',
      closed: 'Closed',
      done: 'Done',
      custom: 'Custom',
    };

    return standardStatuses[normalized]
      || normalized.replace(/\b\w/g, (character) => character.toUpperCase());
  };

  const getPriorityStyle = (priority: string) => {
    switch (priority.toLowerCase()) {
      case 'urgent':
        return 'border-rose-500/40 bg-rose-500/15 text-rose-300';
      case 'high':
        return 'border-orange-500/40 bg-orange-500/15 text-orange-300';
      case 'normal':
        return 'border-blue-500/40 bg-blue-500/15 text-blue-300';
      case 'low':
        return 'border-gray-600 bg-gray-700/40 text-gray-300';
      default:
        return 'border-indigo-500/40 bg-indigo-500/15 text-indigo-300';
    }
  };

  // Resolve the task's current status.
  const getTaskStatus = (task: any): string => {
    // Prefer an explicit current status.
    if (task.currentStatus) return normalizeStatus(task.currentStatus);
    if (task.statusName) return normalizeStatus(task.statusName);
    if (task.status) return normalizeStatus(typeof task.status === 'string' ? task.status : task.status?.status || '');

    // Fall back to the latest status in the task history.
    if (task.columnDetails && typeof task.columnDetails === 'object') {
      const colEntries = Object.entries(task.columnDetails);
      if (colEntries.length === 1) return normalizeStatus(colEntries[0][0]);

      // If the task is completed, prefer a completed status from its history.
      if (task.endDate) {
        const closedCol = [...colEntries].reverse().find(([_, detail]: [string, any]) =>
          detail.statusType === 'closed' || detail.statusType === 'complete' || detail.statusType === 'done'
        );
        if (closedCol) return normalizeStatus(closedCol[0]);
      }

      // Otherwise use the last status in the history.
      if (colEntries.length > 0) {
        return normalizeStatus(colEntries[colEntries.length - 1][0]);
      }
    }

    // Check the scope-change summary as a fallback.
    const scopeMatch = sprintAnalytics?.scopeCreepTasks?.find((s: any) => s.taskId === task.taskId);
    if (scopeMatch?.currentStatus) return normalizeStatus(scopeMatch.currentStatus);

    // Check the stalled-task summary as a fallback.
    const stalledMatch = sprintAnalytics?.stalledTasks?.find((s: any) => s.taskId === task.taskId);
    if (stalledMatch?.currentStatus) return normalizeStatus(stalledMatch.currentStatus);

    return "to do";
  };

  // Resolve the task assignee from available analytics.
  const getTaskAssignee = (task: any) => {
    let assigneeId: string | null = null;
    if (task.assigneeId) {
      assigneeId = String(task.assigneeId);
    } else if (task.userActiveWorkingTimes && Object.keys(task.userActiveWorkingTimes).length > 0) {
      assigneeId = Object.keys(task.userActiveWorkingTimes)[0];
    } else if (task.columnDetails) {
      for (const col of Object.values(task.columnDetails) as any[]) {
        if (col.assigneeTimes && Object.keys(col.assigneeTimes).length > 0) {
          assigneeId = Object.keys(col.assigneeTimes)[0];
          break;
        }
      }
    }

    if (!assigneeId) return null;
    return sprintAnalytics?.usersMap?.[assigneeId] || { id: assigneeId, username: task.assigneeName || `User (${assigneeId})` };
  };

  // Group tasks by their current status.
  const tasksByStatus = useMemo(() => {
    if (!sprintAnalytics?.taskAnalyticsList) return {};

    const grouped: { [statusName: string]: any[] } = {};

    sprintAnalytics.taskAnalyticsList.forEach((task: any) => {
      const normalizedStatus = getTaskStatus(task);
      if (!grouped[normalizedStatus]) {
        grouped[normalizedStatus] = [];
      }
      grouped[normalizedStatus].push(task);
    });

    return grouped;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sprintAnalytics]);

  // Replace neutral status colors with a more distinguishable palette.
  const isGreyColor = (hex?: string): boolean => {
    if (!hex || hex === 'none') return true;
    const clean = hex.replace('#', '').trim();
    if (clean.length === 6) {
      const r = parseInt(clean.substring(0, 2), 16);
      const g = parseInt(clean.substring(2, 4), 16);
      const b = parseInt(clean.substring(4, 6), 16);
      return Math.max(r, g, b) - Math.min(r, g, b) < 32;
    }
    return false;
  };

  // High-contrast colors for common statuses.
  const DISTINCT_STATUS_COLORS: { [key: string]: string } = {
    "to do": "#f59e0b",
    "in progress": "#3b82f6",
    "in review": "#ec4899",
    "complete": "#10b981",
    "closed": "#059669",
  };

  // Accent colors for custom statuses.
  const DYNAMIC_PALETTE = [
    "#8b5cf6",
    "#06b6d4",
    "#f97316",
    "#ec4899",
    "#14b8a6",
    "#e11d48",
    "#6366f1",
  ];

  const resolveColumnColor = (col: any, index: number): string => {
    const norm = (col?.status || "").toLowerCase().trim();

    // 1. Bilinen standart statü eşlemesi
    if (DISTINCT_STATUS_COLORS[norm]) {
      return DISTINCT_STATUS_COLORS[norm];
    }

    // 2. Eğer backend rengi gri DEĞİLSE ve renk varsa onu koru
    if (col?.color && !isGreyColor(col.color)) {
      return col.color;
    }

    // 3. Gri veya tanımsızsa dinamik paletten canlı, belirgin renk ata
    return DYNAMIC_PALETTE[index % DYNAMIC_PALETTE.length];
  };

  // Derive status columns from task data when the API does not provide them.
  const derivedColumns = useMemo(() => {
    if (sprintAnalytics?.availableColumns && sprintAnalytics.availableColumns.length > 0) {
      const sorted = [...sprintAnalytics.availableColumns].sort((a: any, b: any) => a.orderindex - b.orderindex);
      return sorted.map((col: any, idx: number) => ({
        ...col,
        color: resolveColumnColor(col, idx),
      }));
    }
    return Object.keys(tasksByStatus).map((statusKey, idx) => ({
      id: statusKey,
      status: statusKey,
      color: resolveColumnColor({ status: statusKey }, idx),
      orderindex: idx,
    }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sprintAnalytics, tasksByStatus]);

  const totalTasksCount = sprintAnalytics?.taskAnalyticsList?.length || sprintAnalytics?.totalTasks || 0;

  const statusDistribution = useMemo(() => derivedColumns.map((col: any) => {
    const count = (tasksByStatus[normalizeStatus(col.status)] || []).length;
    return {
      id: col.id,
      name: formatStatusName(col.status),
      color: col.color,
      count,
      percentage: totalTasksCount > 0 ? Math.round((count / totalTasksCount) * 100) : 0,
    };
  }), [derivedColumns, tasksByStatus, totalTasksCount]);

  // Team workload and active work-time summary.
  const userWorkTimeChartData = useMemo(() => {
    if (!sprintAnalytics?.usersMap) return [];
    const workTimes = sprintAnalytics.userTotalActiveWorkingTimes || {};
    const taskCounts = sprintAnalytics.userTaskCounts || {};

    // Include every workspace user, including people with no assigned tasks.
    return Object.entries(sprintAnalytics.usersMap).map(([usrId, userObj]: [string, any]) => {
      const workTimeStr = workTimes[usrId] || '0 seconds';
      const taskCount = Number(taskCounts[usrId] || 0);
      return {
        id: usrId,
        name: userObj?.username || `User (${usrId})`,
        workTimeStr,
        taskCount,
        profilePicture: userObj?.profile_picture || null,
      };
    }).sort((a, b) => b.taskCount - a.taskCount);
  }, [sprintAnalytics]);

  const currentUser = userId ? sprintAnalytics?.usersMap?.[userId] : null;

  return (
    <div className="flex h-screen bg-[#0d1117] text-gray-100 overflow-hidden font-sans">

      {/* HOVER SIDEBAR */}
      <aside
        onMouseEnter={() => setIsSidebarHovered(true)}
        onMouseLeave={() => setIsSidebarHovered(false)}
        className={`fixed top-0 left-0 h-full bg-[#161b22] border-r border-gray-800 z-50 transition-all duration-300 ease-in-out flex flex-col justify-between ${isSidebarHovered ? 'w-64 shadow-2xl' : 'w-16'
          }`}
      >
        <div className="flex flex-col flex-1 overflow-hidden">
          {/* User profile */}
          <div className="p-3.5 flex items-center gap-3 border-b border-gray-800">
            <div className="w-9 h-9 rounded-full bg-indigo-950 border border-indigo-500/40 flex items-center justify-center overflow-hidden shrink-0">
              {currentUser?.profile_picture ? (
                <img
                  src={currentUser.profile_picture}
                  alt={currentUser.username}
                  className="w-full h-full object-cover"
                />
              ) : (
                <span className="text-indigo-300 font-bold uppercase text-xs">
                  {currentUser?.username ? currentUser.username.charAt(0) : <User className="w-4 h-4 text-indigo-400" />}
                </span>
              )}
            </div>
            {isSidebarHovered && (
              <div className="truncate">
                <span className="font-bold text-sm text-white block truncate">
                  {currentUser?.username || "Developer"}
                </span>
                <span className="text-[10px] text-gray-400 block truncate">
                  {currentUser?.email || "ClickUp Analytics"}
                </span>
              </div>
            )}
          </div>

          <div className="p-4 flex items-center gap-3 border-b border-gray-800">
            <Layers className="w-6 h-6 text-indigo-400 shrink-0" />
            {isSidebarHovered && <span className="font-bold text-lg text-white whitespace-nowrap">Sprints</span>}
          </div>

          {/* Sprint Listesi */}
          <div className="flex-1 overflow-y-auto p-2 space-y-1">
            {sprintList.map((sprint: any) => (
              <button
                key={sprint.id}
                onClick={() => fetchSprintAnalytics(sprint.id)}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-colors text-left group ${selectedSprintId === sprint.id
                  ? 'bg-indigo-600/30 text-indigo-300 border border-indigo-500/40'
                  : 'hover:bg-indigo-600/20 hover:text-indigo-400'
                  }`}
              >
                <div className={`w-2 h-2 rounded-full shrink-0 ${selectedSprintId === sprint.id ? 'bg-indigo-400' : 'bg-gray-600'
                  }`} />
                {isSidebarHovered && (
                  <span className="text-sm font-medium truncate text-gray-300 group-hover:text-white">
                    {sprint.name}
                  </span>
                )}
              </button>
            ))}
          </div>
        </div>

        {/* Sidebar sign-out button */}
        <div className="p-2 border-t border-gray-800">
          <button
            onClick={handleLogout}
            title="Sign out"
            className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-rose-400 hover:bg-rose-500/10 hover:text-rose-300 transition-colors text-left group"
          >
            <LogOut className="w-5 h-5 shrink-0 text-rose-400 group-hover:scale-110 transition-transform" />
            {isSidebarHovered && (
              <span className="text-sm font-semibold truncate">Sign out</span>
            )}
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="ml-16 flex-1 overflow-y-auto p-4 sm:p-6 xl:p-8">

        {/* Dashboard header */}
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between xl:mb-8">
          <div className="flex flex-col gap-2">
            {availableWorkspaces.length > 0 && (
              <label className="flex w-fit items-center gap-2 text-xs text-gray-400">
                Workspace
                <select
                  value={workspaceId}
                  onChange={(event) => handleWorkspaceChange(event.target.value)}
                  className="rounded-lg border border-gray-700 bg-[#161b22] px-3 py-2 text-sm text-white outline-none focus:border-indigo-500"
                  aria-label="Select workspace"
                >
                  {availableWorkspaces.map((workspace) => (
                    <option key={workspace.id} value={workspace.id}>
                      {workspace.name}
                    </option>
                  ))}
                </select>
              </label>
            )}
            {workspaceLoadError && (
              <p className="text-xs text-rose-400" role="alert">{workspaceLoadError}</p>
            )}

            {/* Selected sprint */}
            <div className="flex items-center gap-3 flex-wrap">
              {sprintAnalytics?.listName ? (
                <div className="flex items-center gap-2.5 bg-gradient-to-r from-indigo-600/20 to-purple-600/10 border border-indigo-500/30 rounded-2xl px-4 py-2 shadow-lg shadow-indigo-500/10">
                  <Zap className="w-4 h-4 text-indigo-400 shrink-0" />
                  <h1 className="text-xl font-extrabold text-white tracking-tight">
                    {sprintAnalytics.listName}
                  </h1>
                </div>
              ) : (
                <h1 className="text-3xl font-bold text-white tracking-tight">Select a sprint</h1>
              )}

              {sprintAnalytics && (
                <span className={`px-3 py-1.5 rounded-full text-xs font-bold border flex items-center gap-1.5 ${isSprintFinished
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                  : 'bg-blue-500/10 text-blue-400 border-blue-500/30 animate-pulse'
                  }`}>
                  {isSprintFinished ? (
                    <><CheckCircle className="w-3.5 h-3.5" /> Completed</>
                  ) : (
                    <><PlayCircle className="w-3.5 h-3.5" /> In progress</>
                  )}
                </span>
              )}
            </div>

            {/* Show dates only when available */}
            {(startDateStr || dueDateStr) && (
              <div className="flex items-center gap-2 text-xs text-gray-400 mt-1 font-mono bg-[#161b22] border border-gray-800 rounded-xl px-3 py-1.5 w-fit">
                <Calendar className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                {startDateStr && dueDateStr ? (
                  <span>{startDateStr} <span className="text-gray-600 mx-1">→</span> {dueDateStr}</span>
                ) : startDateStr ? (
                  <span>Estimated start: {startDateStr}</span>
                ) : (
                  <span>Due: {dueDateStr}</span>
                )}
              </div>
            )}
          </div>

          <div className="flex items-center gap-3">
            <div className="hidden items-center gap-2 text-[10px] text-gray-500 lg:flex" title={streamConnected ? 'Live notifications connected' : 'Connecting to live notifications'}>
              <span className={`h-2 w-2 rounded-full ${streamConnected ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.7)]' : 'bg-amber-400 animate-pulse'}`} />
              {streamConnected ? 'Live' : 'Connecting'}
            </div>
            <div className="bg-[#161b22] border border-gray-800 px-4 py-2 rounded-xl flex items-center gap-3 shadow-sm">
              <Clock className="w-4 h-4 text-emerald-400" />
              <span className="text-xs text-gray-400">Active work time:</span>
              <span className="text-sm font-mono font-bold text-emerald-400">
                {formatDuration(sprintAnalytics?.totalSprintActiveWorkingTime)}
              </span>
            </div>
            <div className="relative">
              <button
                type="button"
                onClick={() => setNotificationPanelOpen((open) => !open)}
                aria-label={`Notifications${unreadNotificationCount ? `, ${unreadNotificationCount} unread` : ''}`}
                aria-expanded={notificationPanelOpen}
                className="relative flex h-10 w-10 items-center justify-center rounded-xl border border-gray-800 bg-[#161b22] text-gray-300 transition hover:border-indigo-500/50 hover:bg-indigo-500/10 hover:text-white"
              >
                <Bell className="h-4 w-4" />
                {unreadNotificationCount > 0 && (
                  <span className="absolute -right-1 -top-1 flex h-[18px] min-w-[18px] items-center justify-center rounded-full border-2 border-[#0d1117] bg-rose-500 px-1 text-[9px] font-bold text-white">
                    {unreadNotificationCount > 99 ? '99+' : unreadNotificationCount}
                  </span>
                )}
              </button>

              {notificationPanelOpen && (
                <section className="absolute right-0 top-full z-[70] mt-2 flex max-h-[min(75vh,36rem)] w-[min(24rem,calc(100vw-5.5rem))] flex-col overflow-hidden rounded-2xl border border-gray-700 bg-[#161b22] shadow-2xl">
                  <div className="flex items-center justify-between border-b border-gray-800 px-4 py-3">
                    <div>
                      <h2 className="text-sm font-bold text-white">Notifications</h2>
                      <p className="mt-0.5 text-[10px] text-gray-500">{unreadNotificationCount} unread</p>
                    </div>
                    <div className="flex items-center gap-1">
                      <button
                        type="button"
                        onClick={() => setNotifications((current) => current.map((item) => ({ ...item, read: true })))}
                        disabled={unreadNotificationCount === 0}
                        className="rounded-lg px-2 py-1.5 text-[10px] font-medium text-indigo-300 hover:bg-indigo-500/10 disabled:cursor-default disabled:text-gray-600"
                      >
                        Mark all read
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setNotifications([]);
                          seenNotificationIds.current.clear();
                          specificTaskEvents.current.clear();
                        }}
                        disabled={notifications.length === 0}
                        aria-label="Clear all notifications"
                        title="Clear all notifications"
                        className="rounded-lg p-1.5 text-gray-500 hover:bg-rose-500/10 hover:text-rose-300 disabled:cursor-default disabled:text-gray-700"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                  {notificationStorageError && (
                    <p className="border-b border-rose-500/20 bg-rose-500/5 px-4 py-2 text-[10px] text-rose-300">
                      Notifications could not be saved in this browser.
                    </p>
                  )}
                  {notifications.length === 0 ? (
                    <div className="px-4 py-10 text-center">
                      <Bell className="mx-auto mb-2 h-5 w-5 text-gray-600" />
                      <p className="text-xs text-gray-400">You’re all caught up</p>
                      <p className="mt-1 text-[10px] text-gray-600">Task updates will appear here.</p>
                    </div>
                  ) : (
                    <div className="overflow-y-auto">
                      {notifications.map((notification) => (
                        <button
                          type="button"
                          key={notification.id}
                          onClick={() => {
                            markNotificationRead(notification.id);
                            if (notification.listId) {
                              void fetchSprintAnalytics(notification.listId);
                              setNotificationPanelOpen(false);
                            }
                          }}
                          className={`flex w-full gap-3 border-b border-gray-800/70 px-4 py-3 text-left transition hover:bg-gray-800/50 ${notification.read ? 'opacity-70' : 'bg-indigo-500/[0.04]'}`}
                        >
                          <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${notification.read ? 'bg-gray-700' : 'bg-indigo-400'}`} />
                          <span className="min-w-0 flex-1">
                            <span className="block truncate text-xs font-semibold text-gray-100">{notification.taskName}</span>
                            <span className="mt-1 block text-[11px] leading-relaxed text-gray-400">{getNotificationMessage(notification)}</span>
                            <span className="mt-1.5 block font-mono text-[9px] text-gray-600">{formatNotificationTime(notification.timestamp)}</span>
                          </span>
                        </button>
                      ))}
                    </div>
                  )}
                </section>
              )}
            </div>
          </div>
        </div>

        {/* Loading indicator */}
        {loading && (
          <div className="flex items-center justify-center py-20">
            <div className="w-10 h-10 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
          </div>
        )}

        {!loading && sprintAnalytics && (
          <>
            <div className="mb-6 rounded-2xl border border-gray-800 bg-gradient-to-br from-[#161b22] to-[#111720] p-4 shadow-lg sm:p-5">
              <div className="mb-4 border-b border-gray-800/80 pb-3">
                <div className="flex items-center gap-2">
                  <Activity className="h-5 w-5 text-indigo-400" />
                  <h3 className="text-base font-bold text-white">Sprint overview</h3>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1.1fr)_minmax(280px,0.9fr)]">
                <section className="min-w-0 rounded-xl border border-gray-800/80 bg-[#0d1117]/45 p-4">
                  <div className="mb-3 flex items-center justify-between gap-3">
                    <div>
                      <h4 className="text-sm font-semibold text-gray-100">Status distribution</h4>
                      <p className="mt-0.5 text-[11px] text-gray-500">Tasks grouped by current status</p>
                    </div>
                    <span className="rounded-lg border border-gray-800 bg-[#161b22] px-2.5 py-1 font-mono text-xs text-gray-300">
                      {totalTasksCount} tasks
                    </span>
                  </div>
                  {statusDistribution.length === 0 ? (
                    <div className="rounded-lg border border-dashed border-gray-800 py-8 text-center text-xs text-gray-500">
                      No status data available
                    </div>
                  ) : (
                    <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
                      {statusDistribution.map((status) => (
                        <div key={status.id} className="rounded-lg border border-gray-800/80 bg-[#161b22]/80 px-3 py-2.5 transition-colors hover:border-gray-700">
                          <div className="mb-2 flex items-center justify-between gap-3">
                            <span className="flex min-w-0 items-center gap-2 text-xs font-medium text-gray-200">
                              <span className="h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: status.color, boxShadow: `0 0 10px ${status.color}66` }} />
                              <span className="truncate">{status.name}</span>
                            </span>
                            <span className="shrink-0 font-mono text-[11px] text-gray-400">
                              <strong className="text-gray-100">{status.count}</strong> · {status.percentage}%
                            </span>
                          </div>
                          <div className="h-1.5 overflow-hidden rounded-full bg-gray-800">
                            <div className="h-full rounded-full transition-all duration-500" style={{ width: `${status.percentage}%`, backgroundColor: status.color }} />
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </section>

                <section className="min-w-0 rounded-xl border border-gray-800/80 bg-[#0d1117]/45 p-4">
                  <div className="mb-3 flex items-center gap-2">
                    <div className="flex h-8 w-8 items-center justify-center rounded-lg border border-indigo-500/20 bg-indigo-500/10">
                      <Users className="h-4 w-4 text-indigo-300" />
                    </div>
                    <div>
                      <h4 className="text-sm font-semibold text-gray-100">Team activity</h4>
                      <p className="text-[11px] text-gray-500">Tasks and active work time</p>
                    </div>
                  </div>
                  {userWorkTimeChartData.length === 0 ? (
                    <div className="rounded-lg border border-dashed border-gray-800 py-8 text-center text-xs text-gray-500">No team data available</div>
                  ) : (
                    <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
                      {userWorkTimeChartData.map((person) => {
                        const maxTasks = Math.max(...userWorkTimeChartData.map((member) => member.taskCount), 1);
                        const barWidth = person.taskCount > 0 ? Math.max((person.taskCount / maxTasks) * 100, 4) : 0;
                        return (
                          <div key={person.id} className="flex items-center gap-3 rounded-lg border border-gray-800/80 bg-[#161b22]/80 px-3 py-2.5">
                            <div className="flex h-8 w-8 shrink-0 items-center justify-center overflow-hidden rounded-full border border-indigo-500/30 bg-indigo-950">
                              {person.profilePicture ? (
                                <img src={person.profilePicture} alt={person.name} className="h-full w-full object-cover" />
                              ) : (
                                <span className="text-[10px] font-bold uppercase text-indigo-300">{person.name.charAt(0)}</span>
                              )}
                            </div>
                            <div className="min-w-0 flex-1">
                              <div className="mb-1 flex items-center justify-between gap-2">
                                <span className="truncate text-xs font-medium text-gray-200">{person.name}</span>
                                <span className="shrink-0 font-mono text-[10px] text-indigo-300">{person.taskCount} tasks</span>
                              </div>
                              <div className="h-1 overflow-hidden rounded-full bg-gray-800">
                                <div className="h-full rounded-full bg-gradient-to-r from-indigo-600 to-indigo-400 transition-all duration-500" style={{ width: `${barWidth}%` }} />
                              </div>
                              <span className="mt-1 block font-mono text-[10px] text-gray-500">{formatDuration(person.workTimeStr)}</span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </section>
              </div>
              {false && (
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-center">
                <div className="flex flex-col sm:flex-row items-center justify-center sm:justify-around gap-6 border-gray-800/60 pr-4 lg:border-r" style={{ minHeight: '190px' }}>
                  <div className="relative w-44 h-44 flex items-center justify-center shrink-0">
                    <svg viewBox="0 0 180 180" className="w-full h-full transform transition-all duration-300">
                      {totalTasksCount === 0 || donutSlices.length === 0 ? (
                        <circle cx="90" cy="90" r="64" fill="none" stroke="#2d3748" strokeWidth="28" />
                      ) : (
                        donutSlices.map((slice) => {
                          const isHovered = hoveredCol === slice.col.status;
                          const showSliceText = slice.angleDelta >= 35;
                          const sliceLabel = slice.col.status;

                          return (
                            <g key={slice.col.id} className="transition-all duration-200">
                              <path
                                d={slice.pathD}
                                fill={slice.color}
                                stroke="#161b22"
                                strokeWidth={isHovered ? 3.5 : 1.5}
                                className={`cursor-pointer transition-all duration-200 ${isHovered
                                  ? 'filter drop-shadow-[0_0_10px_rgba(255,255,255,0.4)] opacity-100'
                                  : 'opacity-95 hover:opacity-100'
                                  }`}
                                onMouseEnter={() => setHoveredCol(slice.col.status)}
                                onMouseLeave={() => setHoveredCol(null)}
                              />
                              {/* DİLİMİN TAM ÜSTÜNDE KOLON ADI */}
                              {showSliceText && (
                                <text
                                  x={slice.textX}
                                  y={slice.textY}
                                  textAnchor="middle"
                                  dominantBaseline="central"
                                  fill="#ffffff"
                                  fontSize="9.5px"
                                  fontWeight="800"
                                  className="pointer-events-none uppercase tracking-wider drop-shadow-[0_1px_3px_rgba(0,0,0,0.95)]"
                                >
                                  {sliceLabel.length > 9 ? sliceLabel.substring(0, 8) + '…' : sliceLabel}
                                </text>
                              )}
                            </g>
                          );
                        })
                      )}

                      {/* İç Merkez Daire */}
                      <circle cx="90" cy="90" r="46" fill="#161b22" stroke="#0d1117" strokeWidth="3" />

                      {/* Merkez Bilgisi */}
                      {(() => {
                        const activeSlice = donutSlices.find(s => s.col.status === hoveredCol);
                        if (activeSlice) {
                          return (
                            <g className="pointer-events-none">
                              <text x="90" y="78" textAnchor="middle" fill={activeSlice.color} fontSize="11px" fontWeight="800" className="uppercase tracking-wider">
                                {activeSlice.col.status.length > 10 ? activeSlice.col.status.substring(0, 9) + '..' : activeSlice.col.status}
                              </text>
                              <text x="90" y="96" textAnchor="middle" fill="#ffffff" fontSize="16px" fontWeight="900">
                                {activeSlice.count} Görev
                              </text>
                              <text x="90" y="109" textAnchor="middle" fill="#9ca3af" fontSize="10px" fontWeight="600" fontFamily="monospace">
                                %{activeSlice.percentage}
                              </text>
                            </g>
                          );
                        }
                        return (
                          <g className="pointer-events-none">
                            <text x="90" y="86" textAnchor="middle" fill="#ffffff" fontSize="22px" fontWeight="900">
                              {totalTasksCount}
                            </text>
                            <text x="90" y="102" textAnchor="middle" fill="#9ca3af" fontSize="9px" fontWeight="700" letterSpacing="0.1em" className="uppercase">
                              Görev
                            </text>
                          </g>
                        );
                      })()}
                    </svg>
                  </div>

                  {/* SAĞ DİNAMİK LEGEND (RENGİN ÜZERİNDE DİREKT KOLON ADI) */}
                  <div className="space-y-2 max-h-48 overflow-y-auto pr-1 w-full sm:w-auto flex-1">
                    {derivedColumns.map((col: any) => {
                      const count = (tasksByStatus[normalizeStatus(col.status)] || []).length;
                      const percentage = totalTasksCount > 0 ? Math.round((count / totalTasksCount) * 100) : 0;
                      const isHovered = hoveredCol === col.status;

                      return (
                        <div
                          key={col.id}
                          onMouseEnter={() => setHoveredCol(col.status)}
                          onMouseLeave={() => setHoveredCol(null)}
                          className={`flex items-center justify-between gap-3 p-2 rounded-xl transition-all border cursor-pointer ${isHovered
                            ? 'bg-gray-800/90 border-indigo-500/50 shadow-md scale-[1.01]'
                            : 'bg-[#0d1117]/60 border-gray-800/60 hover:bg-gray-800/40'
                            }`}
                        >
                          {/* Status name */}
                          <span
                            className="px-2.5 py-1 rounded-lg text-xs font-bold text-white shadow-sm capitalize tracking-wide flex items-center gap-1.5 shrink-0"
                            style={{ backgroundColor: col.color }}
                          >
                            {col.status}
                          </span>

                          <div className="flex items-center gap-2 font-mono text-xs">
                            <span className="font-bold text-white">{count}</span>
                            <span className="text-gray-400 text-[11px]">görev</span>
                            <span className="text-gray-500 text-[10px] font-semibold">%{percentage}</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Kişi bazlı çalışma süresi */}
                <div className="flex flex-col justify-between h-full pl-2">
                  <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-4 flex items-center gap-1.5">
                    <Users className="w-4 h-4 text-indigo-400" />
                    Ekip çalışma süreleri
                  </h4>

                  {userWorkTimeChartData.length === 0 ? (
                    <div className="text-xs text-gray-500 text-center py-8">Kullanıcı verisi bulunamadı.</div>
                  ) : (
                    <div className="space-y-4">
                      {userWorkTimeChartData.map((person) => {
                        const maxTasks = Math.max(...userWorkTimeChartData.map(p => p.taskCount), 1);
                        const barWidth = person.taskCount > 0 ? Math.max((person.taskCount / maxTasks) * 100, 15) : 0;
                        return (
                          <div key={person.id} className="flex items-center gap-3">
                            {/* Avatar */}
                            <div className="w-8 h-8 rounded-full bg-indigo-950 border border-indigo-500/40 flex items-center justify-center overflow-hidden shrink-0">
                              {person.profilePicture ? (
                                <img src={person.profilePicture} alt={person.name} className="w-full h-full object-cover" />
                              ) : (
                                <span className="text-indigo-300 font-bold uppercase text-[10px]">{person.name.charAt(0)}</span>
                              )}
                            </div>
                            {/* İsim + bar + süre */}
                            <div className="flex-1 min-w-0">
                              <div className="flex justify-between items-center mb-1">
                                <span className="text-xs font-semibold text-gray-200 truncate">{person.name}</span>
                                <span className="text-[10px] font-mono text-indigo-300 ml-2 shrink-0">{person.taskCount} görev</span>
                              </div>
                              <div className="w-full bg-gray-800 h-2 rounded-full overflow-hidden">
                                <div
                                  className="bg-gradient-to-r from-indigo-600 to-indigo-400 h-full rounded-full transition-all duration-700"
                                  style={{ width: `${barWidth}%` }}
                                />
                              </div>
                              <span className="text-[10px] text-gray-500 font-mono mt-1 block">{formatDuration(person.workTimeStr)}</span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </div>
              )}
            </div>

            {/* Sprint metrics */}
            <div className="grid grid-cols-2 gap-3 mb-8 sm:grid-cols-3 xl:grid-cols-6">
              {/* Completion */}
              <div className="bg-[#161b22] border border-gray-800 p-5 rounded-2xl flex flex-col justify-between">
                <span className="text-xs text-gray-400 font-medium">Completion</span>
                <p className="text-2xl font-black text-white mt-2">
                  {sprintAnalytics?.completionRatio || `${sprintAnalytics?.completedTasks || 0} / ${sprintAnalytics?.totalTasks || 0}`}
                </p>
                <div className="w-full bg-gray-800 h-1.5 rounded-full mt-3 overflow-hidden">
                  <div
                    className="bg-emerald-500 h-full rounded-full transition-all duration-700"
                    style={{ width: `${((sprintAnalytics?.completedTasks || 0) / (sprintAnalytics?.totalTasks || 1)) * 100}%` }}
                  />
                </div>
              </div>

              {/* Active team members */}
              <div className="bg-[#161b22] border border-gray-800 p-5 rounded-2xl flex flex-col justify-between">
                <span className="text-xs text-gray-400 font-medium">Active team members</span>
                <p className="text-2xl font-black text-indigo-400 mt-2 flex items-end gap-1">
                  {sprintAnalytics?.totalActiveAssignees || 0}
                  <Users className="w-4 h-4 mb-1 text-indigo-500" />
                </p>
                <span className="text-[10px] text-indigo-500/80">People with assigned tasks</span>
              </div>

              {/* Bottleneck status */}
              <div className={`bg-[#161b22] border p-5 rounded-2xl flex flex-col justify-between ${sprintAnalytics?.bottleneckColumn ? 'border-orange-500/40 bg-orange-500/5' : 'border-gray-800'
                }`}>
                <span className="text-xs text-gray-400 font-medium flex items-center gap-1">
                  <TrendingUp className="w-3.5 h-3.5 text-orange-400" /> Bottleneck
                </span>
                <p className="text-sm font-black text-orange-400 mt-2 truncate">
                  {sprintAnalytics?.bottleneckColumn ? formatStatusName(sprintAnalytics.bottleneckColumn) : 'None'}
                </p>
                <span className="text-[10px] text-orange-500/70">Longest average wait</span>
              </div>

              {/* Scope creep */}
              <div className="bg-[#161b22] border border-gray-800 p-5 rounded-2xl flex flex-col justify-between">
                <span className="text-xs text-gray-400 font-medium">Scope creep</span>
                <p className="text-2xl font-black text-amber-400 mt-2">{sprintAnalytics?.scopeCreepTaskCount || 0}</p>
                <span className="text-[10px] text-amber-500/80">Tasks added after sprint start</span>
              </div>

              {/* Stalled tasks */}
              <div className="bg-[#161b22] border border-gray-800 p-5 rounded-2xl flex flex-col justify-between">
                <span className="text-xs text-gray-400 font-medium">Stalled</span>
                <p className="text-2xl font-black text-rose-400 mt-2">
                  {typeof sprintAnalytics?.stalledTaskCount === 'number'
                    ? sprintAnalytics.stalledTaskCount
                    : (sprintAnalytics?.stalledTasks?.length || 0)}
                </p>
                <span className="text-[10px] text-rose-500/80">Inactive for over 2 days</span>
              </div>

              {/* Rollbacks */}
              <div className="bg-[#161b22] border border-gray-800 p-5 rounded-2xl flex flex-col justify-between">
                <span className="text-xs text-gray-400 font-medium">Rollbacks</span>
                <p className="text-2xl font-black text-purple-400 mt-2">{sprintAnalytics?.totalRollbackCount || 0}</p>
                <span className="text-[10px] text-purple-500/80">Status moves backwards</span>
              </div>
            </div>

            {/* Scope-creep tasks */}
            {(sprintAnalytics?.scopeCreepTasks?.length ?? 0) > 0 && (
              <div className="bg-[#161b22] border border-amber-500/20 rounded-2xl mb-6 overflow-hidden">
                <button
                  onClick={() => setScopeCreepExpanded(v => !v)}
                  className="w-full flex items-center justify-between px-5 py-3.5 text-left hover:bg-amber-500/5 transition-colors"
                >
                  <div className="flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 text-amber-400" />
                    <span className="text-sm font-bold text-amber-300">Scope-creep tasks</span>
                    <span className="px-2 py-0.5 text-[10px] font-bold bg-amber-500/20 text-amber-400 rounded-full">
                      {sprintAnalytics.scopeCreepTasks.length}
                    </span>
                  </div>
                  {scopeCreepExpanded
                    ? <ChevronUp className="w-4 h-4 text-gray-400" />
                    : <ChevronDown className="w-4 h-4 text-gray-400" />}
                </button>
                {scopeCreepExpanded && (
                  <div className="px-5 pb-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                    {sprintAnalytics.scopeCreepTasks.map((t: any) => {
                      const col = derivedColumns.find((c: any) => c.status.toLowerCase() === t.currentStatus?.toLowerCase());
                      const colColor = col?.color || '#6366f1';
                      return (
                        <div key={t.taskId} className="flex items-center justify-between bg-[#0d1117] border border-gray-800 rounded-xl px-3 py-2.5">
                          <span className="text-xs font-medium text-gray-300 truncate max-w-[70%]">{t.taskName}</span>
                          <span
                            className="text-[10px] font-bold px-2 py-0.5 rounded-full capitalize ml-2 shrink-0"
                            style={{ backgroundColor: `${colColor}20`, color: colColor, border: `1px solid ${colColor}40` }}
                          >
                            {formatStatusName(t.currentStatus)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            )}

            {/* Kanban board */}
            <div className="space-y-4">
              <h2 className="text-xl font-bold text-white tracking-tight flex items-center gap-2">
                <Activity className="w-5 h-5 text-indigo-400" />
                Sprint board
              </h2>

              {derivedColumns.length === 0 ? (
                <div className="text-sm text-gray-500 text-center py-12 bg-[#161b22] rounded-2xl border border-gray-800">
                  No status columns available.
                </div>
              ) : (
                <div className="flex items-start gap-3 overflow-x-auto pb-4">
                  {derivedColumns.map((col: any) => {
                    const statusKey = normalizeStatus(col.status);
                    const tasksInCol = tasksByStatus[statusKey] || [];
                    const columnColor = col.color || '#6366f1';

                    return (
                      <div
                        key={col.id}
                        className={`w-[200px] shrink-0 rounded-xl border bg-[#161b22] p-3 flex flex-col h-fit min-h-[76px] max-h-[560px] shadow-lg sm:w-[220px] ${sprintAnalytics?.bottleneckColumn?.toLowerCase() === col.status.toLowerCase()
                          ? 'border-orange-500/40'
                          : 'border-gray-800'
                          }`}
                        style={{ borderTopColor: columnColor, borderTopWidth: '2px' }}
                      >
                        <div className="flex justify-between items-center pb-3 border-b border-gray-800/80 mb-3">
                          <div className="flex items-center gap-2">
                            {/* RENGİN ÜSTÜNDE DİREKT KOLON ADI */}
                            <span
                              className="max-w-[150px] truncate rounded-md px-2 py-1 text-[11px] font-bold text-white shadow-sm"
                              style={{ backgroundColor: columnColor }}
                            >
                              {formatStatusName(col.status)}
                            </span>
                            {sprintAnalytics?.bottleneckColumn?.toLowerCase() === col.status.toLowerCase() && (
                              <span className="text-[9px] font-bold px-1.5 py-0.5 bg-orange-500/20 text-orange-400 border border-orange-500/30 rounded flex items-center gap-0.5">
                                <TrendingUp className="w-2.5 h-2.5" /> BOTTLENECK
                              </span>
                            )}
                          </div>
                          <span className="px-2.5 py-0.5 text-xs font-bold font-mono rounded-full bg-[#0d1117] text-gray-300 border border-gray-800">
                            {tasksInCol.length}
                          </span>
                        </div>

                        <div className={`${tasksInCol.length > 0 ? 'max-h-[480px] overflow-y-auto space-y-3 pr-1' : ''}`}>
                          {tasksInCol.length === 0 ? (
                            <div className="text-xs text-gray-600 text-center py-3 italic">
                              No tasks in this status
                            </div>
                          ) : (
                            tasksInCol.map((task: any) => {
                              const assigneeUser = getTaskAssignee(task);
                              const userAvatar = assigneeUser?.profile_picture;
                              const displayName = assigneeUser?.username;

                              return (
                                <div
                                  key={task.taskId}
                                  onClick={() => void openTaskDetails(task)}
                                  className="bg-[#0d1117] hover:bg-[#1f2430] border border-gray-800 hover:border-indigo-500/50 p-3 rounded-lg transition-all cursor-pointer group shadow-sm flex flex-col justify-between space-y-3"
                                >
                                  {/* Task title and badges */}
                                  <div className="space-y-1.5">
                                    <p className="font-medium text-sm text-gray-200 group-hover:text-indigo-300 transition-colors line-clamp-2">
                                      {task.taskName}
                                    </p>
                                    <div className="flex flex-wrap gap-1">
                                      {task.priority && (
                                        <span className={`rounded border px-1.5 py-0.5 text-[9px] font-bold capitalize ${getPriorityStyle(task.priority)}`}>
                                          {task.priority}
                                        </span>
                                      )}
                                      {task.scopeCreep && (
                                        <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-rose-500/15 text-rose-400 border border-rose-500/30 flex items-center gap-0.5">
                                          <AlertTriangle className="w-2.5 h-2.5" /> Scope creep
                                        </span>
                                      )}
                                      {task.stalled && (
                                        <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-rose-500/15 text-rose-400 border border-rose-500/30">
                                          Stalled
                                        </span>
                                      )}
                                      {task.blockedByTaskId && (
                                        <span className="text-[9px] font-bold px-1.5 py-0.5 rounded bg-rose-500/20 text-rose-300 border border-rose-500/30">
                                          Blocked
                                        </span>
                                      )}
                                    </div>
                                  </div>

                                  <div className="flex justify-between items-center pt-2 border-t border-gray-800/60">
                                    <span className="text-[11px] font-mono text-gray-400 flex items-center gap-1">
                                      <Clock className="w-3 h-3 text-indigo-400" />
                                      {formatDuration(task.totalActiveWorkingTime)}
                                    </span>

                                    <div className="w-6 h-6 rounded-full bg-indigo-950 border border-indigo-500/40 flex items-center justify-center overflow-hidden shrink-0">
                                      {userAvatar ? (
                                        <img src={userAvatar} alt={displayName || 'User'} className="w-full h-full object-cover" />
                                      ) : (
                                        <span className="text-[10px] font-bold text-indigo-300 uppercase">
                                          {displayName ? displayName.charAt(0) : <User className="w-3 h-3" />}
                                        </span>
                                      )}
                                    </div>
                                  </div>
                                </div>
                              );
                            })
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </>
        )}

        {/* Empty state */}
        {!loading && !sprintAnalytics && (
          <div className="flex flex-col items-center justify-center h-64 text-gray-600">
            <Layers className="w-12 h-12 mb-3 text-gray-700" />
            <p className="text-sm">Select a sprint from the sidebar</p>
          </div>
        )}
      </main>

      {toastNotification && (
        <div className="fixed bottom-5 right-5 z-[80] w-[min(24rem,calc(100vw-2.5rem))] animate-in slide-in-from-bottom-2 rounded-xl border border-indigo-500/30 bg-[#161b22] p-4 shadow-2xl">
          <div className="flex items-start gap-3">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-indigo-500/15 text-indigo-300">
              <Bell className="h-4 w-4" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-[10px] font-semibold uppercase tracking-wider text-indigo-300">Task update</p>
              <p className="mt-1 truncate text-xs font-semibold text-white">{toastNotification.taskName}</p>
              <p className="mt-0.5 text-[11px] text-gray-400">{getNotificationMessage(toastNotification)}</p>
            </div>
            <button type="button" onClick={() => setToastNotification(null)} aria-label="Dismiss notification" className="text-gray-500 hover:text-white">
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}

      {/* Task details drawer */}
      {selectedTask && (
        <div className="fixed inset-0 z-50 flex justify-end">
          {/* Drawer backdrop */}
          <div
            className="fixed inset-0 bg-black/60 backdrop-blur-xs transition-opacity cursor-pointer"
            onClick={() => setSelectedTask(null)}
          />

          {/* Drawer content */}
          <div className="relative w-full max-w-[500px] h-full bg-[#161b22] border-l border-gray-800 shadow-2xl p-6 overflow-y-auto z-10 flex flex-col space-y-6">
            <div className="flex justify-between items-center pb-4 border-b border-gray-800 shrink-0">
              <h3 className="font-bold text-lg text-white flex items-center gap-2">
                <GitCommit className="w-5 h-5 text-indigo-400" />
                Task lifecycle
              </h3>
              <button
                onClick={() => setSelectedTask(null)}
                className="p-1 hover:bg-gray-800 rounded-lg text-gray-400 hover:text-white transition-colors"
                title="Close"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-6">
              {/* Task details */}
              <div className="bg-[#0d1117] p-4 rounded-xl border border-gray-800/80 space-y-3">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-[11px] text-gray-400 uppercase font-semibold tracking-wider">Task details</span>
                  <span className="text-xs font-mono text-gray-400 bg-gray-800 px-2 py-0.5 rounded border border-gray-700">
                    ID: {selectedTask.taskId}
                  </span>
                </div>

                <p className="text-base font-bold text-white leading-snug">
                  {selectedTask.taskName}
                </p>

                {/* Görevin Adresi / Linki */}
                <div className="pt-1">
                  <a
                    href={`https://app.clickup.com/t/${selectedTask.taskId}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1.5 text-xs text-indigo-400 hover:text-indigo-300 bg-indigo-500/10 hover:bg-indigo-500/20 border border-indigo-500/30 px-2.5 py-1 rounded-lg transition-colors font-mono max-w-full truncate"
                    title="Open in ClickUp"
                  >
                    <ExternalLink className="w-3.5 h-3.5 shrink-0" />
                    <span className="truncate">https://app.clickup.com/t/{selectedTask.taskId}</span>
                  </a>
                </div>

                {/* Status badges */}
                <div className="flex flex-wrap gap-1.5 pt-1">
                  <span className="text-xs font-semibold px-2.5 py-1 rounded-full capitalize bg-indigo-500/10 text-indigo-300 border border-indigo-500/30">
                    {formatStatusName(getTaskStatus(selectedTask))}
                  </span>
                  <span className={`rounded-full border px-2.5 py-1 text-xs font-bold ${selectedTask.priority
                    ? `capitalize ${getPriorityStyle(selectedTask.priority)}`
                    : 'border-gray-700 text-gray-400'}`}>
                    Priority: {selectedTask.priority || 'Not set'}
                  </span>
                  {selectedTask.scopeCreep && (
                    <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/40 flex items-center gap-1.5 shadow-sm">
                      <AlertTriangle className="w-3.5 h-3.5 text-rose-400" /> Scope creep
                    </span>
                  )}
                  {selectedTask.stalled && (
                    <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/40 flex items-center gap-1.5 shadow-sm">
                      <Ban className="w-3.5 h-3.5 text-rose-400" /> Stalled
                    </span>
                  )}
                  {selectedTask.blockedByTaskId && (
                    <span className="text-xs font-bold px-2.5 py-1 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/40 flex items-center gap-1.5 shadow-sm">
                      <ShieldAlert className="w-3.5 h-3.5 text-rose-400" /> Blocked
                    </span>
                  )}
                </div>
              </div>

              {/* Blocking task */}
              {selectedTask.blockedByTaskId && (
                <div className="bg-rose-950/30 border border-rose-500/40 rounded-xl p-3.5 flex items-start gap-3">
                  <ShieldAlert className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                  <div className="text-xs">
                    <span className="font-bold text-rose-300 block">Blocked by task</span>
                    <p className="text-gray-300 mt-0.5">
                      This task is blocked by <span className="font-mono font-bold text-rose-400">#{selectedTask.blockedByTaskId}</span>.
                    </p>
                  </div>
                </div>
              )}

              {/* Assignee */}
              {(() => {
                const assignee = getTaskAssignee(selectedTask);
                return assignee ? (
                  <div className="bg-[#0d1117] p-3 rounded-xl border border-gray-800 flex items-center gap-3">
                    <UserAvatar username={assignee.username} profilePicture={assignee.profile_picture} size={36} />
                    <div className="min-w-0">
                      <span className="text-[10px] text-gray-400 block font-medium uppercase tracking-wider">Assignee</span>
                      <span className="text-xs font-semibold text-white truncate block">{assignee.username}</span>
                      {assignee.email && <span className="text-[10px] text-gray-500 truncate block">{assignee.email}</span>}
                    </div>
                  </div>
                ) : null;
              })()}

              {/* Work time, rollbacks, start and due dates */}
              <div className="grid grid-cols-2 gap-3">
                <div className="bg-[#0d1117] p-3 rounded-xl border border-gray-800 flex flex-col justify-between">
                  <span className="text-[11px] text-gray-400 flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5 text-indigo-400" /> Active work time
                  </span>
                  <p className="text-sm font-mono font-bold text-indigo-400 mt-1">
                    {formatDuration(selectedTask.totalActiveWorkingTime)}
                  </p>
                </div>

                <div className="bg-[#0d1117] p-3 rounded-xl border border-gray-800 flex flex-col justify-between">
                  <span className="text-[11px] text-gray-400 flex items-center gap-1.5">
                    <RotateCcw className="w-3.5 h-3.5 text-purple-400" /> Rollbacks
                  </span>
                  <p className={`text-sm font-mono font-bold mt-1 ${selectedTask.rollbackCount > 0 ? 'text-amber-400' : 'text-gray-300'}`}>
                    {selectedTask.rollbackCount || 0}
                  </p>
                </div>

                <div className="bg-[#0d1117] p-3 rounded-xl border border-gray-800 flex flex-col justify-between">
                  <span className="text-[11px] text-gray-400 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-blue-400" /> Start date
                  </span>
                  <p className="text-xs font-mono font-medium text-gray-200 mt-1">
                    {formatDateTime(selectedTask.startDate) || 'Not specified'}
                  </p>
                </div>

                <div className="bg-[#0d1117] p-3 rounded-xl border border-gray-800 flex flex-col justify-between">
                  <span className="text-[11px] text-gray-400 flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-emerald-400" /> Due date
                  </span>
                  <p className="text-xs font-mono font-medium text-gray-200 mt-1">
                    {formatDateTime(selectedTask.endDate) || (
                      <span className="text-amber-400 font-sans">In progress</span>
                    )}
                  </p>
                </div>
              </div>

              {/* Team member work time */}
              {selectedTask.userActiveWorkingTimes && Object.keys(selectedTask.userActiveWorkingTimes).length > 0 && (
                <div>
                  <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2.5 flex items-center gap-1.5">
                    <Users className="w-3.5 h-3.5 text-indigo-400" />
                    Total work time by team member
                  </h4>
                  <div className="space-y-2 bg-[#0d1117] p-3 rounded-xl border border-gray-800">
                    {Object.entries(selectedTask.userActiveWorkingTimes).map(([uId, timeStr]: [string, any]) => {
                      const u = sprintAnalytics?.usersMap?.[uId];
                      const displayName = u?.username || u?.email || `User (${uId})`;
                      return (
                        <div key={uId} className="flex justify-between items-center text-xs bg-[#161b22] px-3 py-2 rounded-lg border border-gray-800">
                          <div className="flex items-center gap-2.5 min-w-0">
                            <UserAvatar username={displayName} profilePicture={u?.profile_picture} size={24} />
                            <div className="min-w-0">
                              <span className="text-gray-200 font-medium block truncate max-w-[200px]" title={displayName}>
                                {displayName}
                              </span>
                              {u?.email && <span className="text-[10px] text-gray-500 block truncate max-w-[200px]">{u.email}</span>}
                            </div>
                          </div>
                          <span className="font-mono text-indigo-400 font-semibold bg-indigo-500/10 px-2.5 py-1 rounded border border-indigo-500/20 shrink-0">
                            {formatDuration(timeStr)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Status history timeline */}
              <div>
                <div className="mb-4">
                  <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wider flex items-center gap-1.5">
                    <Layers className="w-3.5 h-3.5 text-indigo-400" />
                    Status history and lifecycle
                  </h4>
                  <p className="text-[11px] text-gray-500 mt-0.5">
                    Statuses, time spent, and assigned team members throughout this task:
                  </p>
                </div>

                {(() => {
                  const colEntries = Object.entries(selectedTask.columnDetails || {});
                  if (colEntries.length === 0) {
                    return (
                      <div className="bg-[#0d1117] p-4 rounded-xl border border-gray-800 text-center text-xs text-gray-500 italic">
                        No status history is available for this task.
                      </div>
                    );
                  }

                  return (
                    <div className="relative pl-6 space-y-4 before:absolute before:left-3 before:top-3 before:bottom-3 before:w-0.5 before:bg-gradient-to-b before:from-gray-700 before:via-gray-800 before:to-gray-900">
                      {colEntries.map(([colName, details]: [string, any]) => {
                        const cfg = getStatusTypeConfig(details.statusType);
                        const assigneeList = Object.entries(details.assigneeTimes || {});

                        return (
                          <div key={colName} className="relative group">
                            {/* Timeline marker */}
                            <div className={`absolute -left-6 top-3 w-3 h-3 rounded-full ring-4 ring-[#161b22] ${cfg.dotColor} transition-transform group-hover:scale-125`} />

                            {/* Status card */}
                            <div className={`bg-[#0d1117] p-3.5 rounded-xl border transition-all ${cfg.borderAccent} hover:border-gray-600`}>
                              {/* Status and type */}
                              <div className="flex items-center justify-between gap-2 flex-wrap mb-2">
                                <span className={`text-xs font-bold uppercase tracking-wide ${cfg.titleColor}`}>
                                  {formatStatusName(colName)}
                                </span>
                                <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${cfg.badgeBg} ${cfg.badgeText} ${cfg.badgeBorder}`}>
                                  {cfg.label}
                                </span>
                              </div>

                              {/* Time in status */}
                              <div className="flex items-center justify-between text-xs bg-[#161b22] px-2.5 py-1.5 rounded-lg border border-gray-800/80 mb-2.5">
                                <span className="text-gray-400 flex items-center gap-1.5 font-medium">
                                  <Clock className="w-3 h-3 text-indigo-400" />
                                  Time in status:
                                </span>
                                <span className="font-mono font-bold text-emerald-400">
                                  {formatDuration(details.totalDurationFormatted)}
                                </span>
                              </div>

                              {/* Assignees in this status */}
                              <div className="pt-2 border-t border-gray-800/60">
                                <span className="text-[10px] font-semibold text-gray-500 uppercase tracking-wider block mb-1.5">
                                  Assignees and work time
                                </span>
                                {assigneeList.length > 0 ? (
                                  <div className="space-y-1.5">
                                    {assigneeList.map(([uId, timeStr]: [string, any]) => {
                                      const u = sprintAnalytics?.usersMap?.[uId];
                                      const displayName = u?.username || u?.email || `User (${uId})`;
                                      return (
                                        <div key={uId} className="flex justify-between items-center text-xs bg-[#161b22] px-2.5 py-1.5 rounded-lg border border-gray-800/70">
                                          <div className="flex items-center gap-2 min-w-0">
                                            <UserAvatar username={displayName} profilePicture={u?.profile_picture} size={20} />
                                            <span className="text-gray-300 font-medium truncate max-w-[160px]" title={displayName}>
                                              {displayName}
                                            </span>
                                          </div>
                                          <span className="font-mono font-semibold text-indigo-400 shrink-0">
                                            {formatDuration(timeStr)}
                                          </span>
                                        </div>
                                      );
                                    })}
                                  </div>
                                ) : (
                                  <p className="text-[11px] text-gray-500 italic">
                                    No specific assignee was recorded for this status.
                                  </p>
                                )}
                              </div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  );
                })()}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}