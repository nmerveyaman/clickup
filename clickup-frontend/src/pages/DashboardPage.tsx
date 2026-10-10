import { useState } from 'react';
import {
  Layers,
  BarChart3,
  LogOut,
  ChevronRight,
  ChevronDown,
  Folder,
  ListTodo,
  Building2,
  FolderKanban,
  Activity,
  Calendar,
} from 'lucide-react';
//import { analyticsApi } from '../services/analyticsApi';

interface DashboardPageProps {
  user: {
    userId: number;
    username: string;
    role: string;
  };
  onLogout: () => void;
}

// Örnek hiyerarşi verisi
const WORKSPACE_TREE = {
  workspaceName: 'Teknoloji & Ar-Ge Workspace',
  spaces: [
    {
      spaceId: 'spc_1',
      spaceName: 'Ürün Geliştirme Space',
      folders: [
        {
          folderId: 'fld_1',
          folderName: '2026 Q4 Sprintleri',
          lists: [
            { listId: '90120485', listName: 'Sprint 14 - Core Backend & BLE Sync' },
            { listId: '90120486', listName: 'Sprint 15 - ClickUp Analytics UI' },
          ],
        },
      ],
    },
  ],
};

export default function DashboardPage({ user, onLogout }: DashboardPageProps) {
  const [activeTab, setActiveTab] = useState<'sprint' | 'task' | 'gantt'>('sprint');
  const [openSpaces, setOpenSpaces] = useState<Record<string, boolean>>({ spc_1: true });
  const [openFolders, setOpenFolders] = useState<Record<string, boolean>>({ fld_1: true });
  const [selectedList, setSelectedList] = useState({
    spaceName: 'Ürün Geliştirme Space',
    folderName: '2026 Q4 Sprintleri',
    listId: '90120485',
    listName: 'Sprint 14 - Core Backend & BLE Sync',
  });

  return (
    <div className="min-h-screen bg-slate-50 text-slate-800 flex">
      {/* Sol Yan Menü */}
      <aside className="w-72 bg-slate-900 text-slate-300 flex flex-col border-r border-slate-800 shrink-0 select-none">
        <div className="p-5 border-b border-slate-800 flex items-center gap-3">
          <div className="bg-indigo-600 p-2 rounded-xl">
            <BarChart3 className="w-5 h-5 text-white" />
          </div>
          <div>
            <span className="font-bold text-white block leading-none">ClickUp Analytics</span>
            <span className="text-[11px] text-slate-400">Sprint & Efor Yönetimi</span>
          </div>
        </div>

        {/* Aktif Kullanıcı Kartı */}
        <div className="p-4 border-b border-slate-800 bg-slate-800/40">
          <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 block mb-1">
            Giriş Yapan Kullanıcı
          </span>
          <div className="text-sm font-semibold text-white truncate">
            {user.username} <span className="text-xs font-normal text-indigo-400">({user.role})</span>
          </div>
        </div>

        <div className="p-4 border-b border-slate-800">
          <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 block mb-1.5">
            Aktif Workspace
          </span>
          <div className="flex items-center gap-2.5 bg-slate-800/90 border border-slate-700 p-2.5 rounded-xl text-sm font-medium text-white">
            <Building2 className="w-4 h-4 text-indigo-400 shrink-0" />
            <span className="truncate">{WORKSPACE_TREE.workspaceName}</span>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-3 space-y-2">
          <span className="text-[10px] font-semibold uppercase tracking-wider text-slate-500 px-2 block">
            Çalışma Alanları (Spaces)
          </span>

          {WORKSPACE_TREE.spaces.map((space) => (
            <div key={space.spaceId} className="space-y-1">
              <button
                onClick={() =>
                  setOpenSpaces((prev) => ({ ...prev, [space.spaceId]: !prev[space.spaceId] }))
                }
                className="w-full flex items-center gap-2 px-2.5 py-2 rounded-lg text-sm font-medium text-slate-200 hover:bg-slate-800/60 transition-colors cursor-pointer"
              >
                {openSpaces[space.spaceId] ? (
                  <ChevronDown className="w-4 h-4 text-slate-400 shrink-0" />
                ) : (
                  <ChevronRight className="w-4 h-4 text-slate-400 shrink-0" />
                )}
                <Layers className="w-4 h-4 text-indigo-400 shrink-0" />
                <span className="truncate">{space.spaceName}</span>
              </button>

              {openSpaces[space.spaceId] && (
                <div className="pl-4 space-y-1 border-l border-slate-800 ml-4">
                  {space.folders.map((folder) => (
                    <div key={folder.folderId} className="space-y-1">
                      <button
                        onClick={() =>
                          setOpenFolders((prev) => ({
                            ...prev,
                            [folder.folderId]: !prev[folder.folderId],
                          }))
                        }
                        className="w-full flex items-center gap-2 px-2.5 py-1.5 rounded-lg text-xs font-medium text-slate-300 hover:bg-slate-800/50 transition-colors cursor-pointer"
                      >
                        {openFolders[folder.folderId] ? (
                          <ChevronDown className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                        ) : (
                          <ChevronRight className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                        )}
                        <Folder className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                        <span className="truncate">{folder.folderName}</span>
                      </button>

                      {openFolders[folder.folderId] && (
                        <div className="pl-4 space-y-1 border-l border-slate-800/80 ml-3">
                          {folder.lists.map((list) => {
                            const isSelected = selectedList.listId === list.listId;
                            return (
                              <button
                                key={list.listId}
                                onClick={() =>
                                  setSelectedList({
                                    spaceName: space.spaceName,
                                    folderName: folder.folderName,
                                    listId: list.listId,
                                    listName: list.listName,
                                  })
                                }
                                className={`w-full flex items-center gap-2 px-2.5 py-2 rounded-lg text-xs transition-all cursor-pointer ${
                                  isSelected
                                    ? 'bg-indigo-600 text-white font-semibold shadow-sm'
                                    : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                                }`}
                              >
                                <ListTodo className="w-3.5 h-3.5 shrink-0" />
                                <span className="truncate text-left">{list.listName}</span>
                              </button>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>

        <div className="p-4 border-t border-slate-800">
          <button
            onClick={onLogout}
            className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-xl text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Bağlantıyı Kes / Çıkış</span>
          </button>
        </div>
      </aside>

      {/* Sağ Ana Panel */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="bg-white border-b border-slate-200 px-8 py-4 flex flex-wrap items-center justify-between gap-4 sticky top-0 z-10">
          <div>
            <div className="flex items-center gap-1.5 text-xs text-slate-400 font-medium">
              <span>{selectedList.spaceName}</span>
              <ChevronRight className="w-3.5 h-3.5" />
              <span>{selectedList.folderName}</span>
              <ChevronRight className="w-3.5 h-3.5" />
              <span className="text-indigo-600 font-semibold">{selectedList.listName}</span>
            </div>
            <h1 className="text-xl font-bold text-slate-900 mt-0.5">{selectedList.listName}</h1>
          </div>

          <div className="flex bg-slate-100 p-1 rounded-xl border border-slate-200">
            <button
              onClick={() => setActiveTab('sprint')}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all cursor-pointer ${
                activeTab === 'sprint' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-600'
              }`}
            >
              <FolderKanban className="w-4 h-4" />
              <span>Sprint Özeti</span>
            </button>
            <button
              onClick={() => setActiveTab('task')}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all cursor-pointer ${
                activeTab === 'task' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-600'
              }`}
            >
              <Activity className="w-4 h-4" />
              <span>Görev Analizi</span>
            </button>
            <button
              onClick={() => setActiveTab('gantt')}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all cursor-pointer ${
                activeTab === 'gantt' ? 'bg-white text-indigo-600 shadow-sm' : 'text-slate-600'
              }`}
            >
              <Calendar className="w-4 h-4" />
              <span>Geliştirici Gantt</span>
            </button>
          </div>
        </header>

        <main className="p-8 space-y-6 max-w-7xl w-full mx-auto">
          <div className="bg-white border border-slate-200 rounded-2xl p-8 text-center shadow-sm">
            <h2 className="text-lg font-bold text-slate-800 mb-2">
              Seçilen Sprint: {selectedList.listName}
            </h2>
            <p className="text-sm text-slate-500">
              Hoş geldin, <strong>{user.username}</strong>! Backend bağlantısı aktif, artık bu ekranda seçtiğin sprintlerin verilerini Spring Boot üzerinden çekebilirsin. 🚀
            </p>
          </div>
        </main>
      </div>
    </div>
  );
}