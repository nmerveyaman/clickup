import axios from 'axios';
import { BASE_URL } from '../constants/apiUrls';

const API_BASE_URL = `${BASE_URL}/api/v1`;

export interface ListHierarchy {
  id: string;
  name: string;
  taskCount: number;
}

export interface FolderHierarchy {
  id: string;
  name: string;
  lists: ListHierarchy[];
}

export interface SpaceHierarchy {
  id: string;
  name: string;
  folders: FolderHierarchy[];
  folderlessLists: ListHierarchy[];
}

export interface WorkspaceHierarchy {
  id: string;
  name: string;
  spaces: SpaceHierarchy[];
}

export interface TaskDetail {
  id: string;
  name: string;
  statusName: string;
  statusType: string;
  dateCreated: number;
  dateUpdated: number;
}

export const fetchHierarchy = async (): Promise<WorkspaceHierarchy[]> => {
  const response = await axios.get(`${API_BASE_URL}/workspaces/hierarchy`);
  return response.data;
};

export const fetchTasksByList = async (listId: string): Promise<TaskDetail[]> => {
  const response = await axios.get(`${API_BASE_URL}/lists/${listId}/tasks`);
  return response.data;
};