// src/components/AnalyticsComponents.tsx
import React from 'react';
import type {AnalyticsData,AvailableColumn} from '../types/analyticstypes';

interface ComponentProps {
  data: AnalyticsData;
}

// 1. Dinamik Kolon Listesi Bileşeni
export const DynamicColumnsList: React.FC<ComponentProps> = ({ data }) => {
  if (!data?.availableColumns) return null;

  // Kolonları orderindex değerine göre sıralıyoruz
  const sortedColumns = [...data.availableColumns].sort(
    (a, b) => a.orderindex - b.orderindex
  );

  return (
    <div className="flex flex-wrap gap-3 my-4">
      {sortedColumns.map((col: AvailableColumn) => {
        const count = data.taskCountPerColumn[col.status] || 0;

        return (
          <div
            key={col.id}
            className="flex items-center gap-2 px-3.5 py-2 rounded-xl border text-sm font-semibold shadow-sm transition-all"
            style={{
              borderColor: col.color,
              backgroundColor: `${col.color}15`,
              color: col.color,
            }}
          >
            <span
              className="w-2.5 h-2.5 rounded-full"
              style={{ backgroundColor: col.color }}
            />
            <span className="capitalize">{col.status}</span>
            <span className="ml-1 px-2 py-0.5 text-xs font-bold bg-slate-800 text-white rounded-full">
              {count}
            </span>
          </div>
        );
      })}
    </div>
  );
};

// 2. Kullanıcı Task Kartı (Arayüzündeki 'yeni' Kart Tasarımı)
export const UserTaskCard: React.FC<{ userId: string; data: AnalyticsData }> = ({
  userId,
  data,
}) => {
  const user = data.usersMap[userId];
  const taskCount = data.userTaskCounts[userId] || 0;

  return (
    <div className="bg-[#0f172a] text-white p-4 rounded-xl w-64 shadow-md border border-slate-800 flex flex-col justify-between">
      {/* Üst Kısım: Kullanıcı Adı */}
      <div className="border-b border-slate-800/80 pb-2 mb-3">
        <h3 className="text-base font-bold text-slate-100 truncate">
          {user?.username || `Kullanıcı (${userId})`}
        </h3>
      </div>

      {/* Alt Kısım: Task Sayısı ve Profil / Avatar */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5 text-slate-400 text-sm font-medium">
          <span className="text-blue-400 font-bold text-base">{taskCount}</span>
          <span>Görev</span>
        </div>

        {/* Profil Resmi Varsa Resim, Yoksa Varsayılan İkon / Harf */}
        <div className="w-9 h-9 rounded-full bg-indigo-950 border border-indigo-500/30 flex items-center justify-center overflow-hidden">
          {user?.profile_picture ? (
            <img
              src={user.profile_picture}
              alt={user.username}
              className="w-full h-full object-cover"
            />
          ) : (
            <span className="text-indigo-300 font-bold uppercase text-xs">
              {user?.username ? user.username.charAt(0) : 'U'}
            </span>
          )}
        </div>
      </div>
    </div>
  );
};

// 3. Tüm Kullanıcı Kartlarını Listeleme Bileşeni
export const UserTaskList: React.FC<ComponentProps> = ({ data }) => {
  if (!data?.userTaskCounts) return null;

  return (
    <div className="flex flex-wrap gap-4 my-4">
      {Object.keys(data.userTaskCounts).map((userId) => (
        <UserTaskCard key={userId} userId={userId} data={data} />
      ))}
    </div>
  );
};