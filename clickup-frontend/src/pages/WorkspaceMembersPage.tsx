import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Users, Mail, Shield, User, ArrowLeft, RefreshCw } from 'lucide-react';
import { BASE_URL } from '../constants/apiUrls';

interface Member {
  id: number | string;
  username: string;
  email: string;
  profile_picture: string | null;
  appRole?: string;
  role_key?: string;
}

export default function WorkspaceMembersPage() {
  const navigate = useNavigate();
  const [members, setMembers] = useState<Member[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const queryParams = new URLSearchParams(window.location.search);
  const workspaceId = queryParams.get('workspaceId') || localStorage.getItem('workspaceId');

  useEffect(() => {
    if (!workspaceId) return;

    const fetchMembers = async () => {
      setLoading(true);
      try {
        const res = await fetch(`${BASE_URL}/api/v1/workspaces/${workspaceId}/members`);
        if (res.ok) {
          const data = await res.json();
          setMembers(data);
        }
      } catch (err) {
        console.error('Ekip üyeleri çekilemedi:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchMembers();
  }, [workspaceId]);

  return (
    <div className="min-h-screen bg-[#0d1117] text-gray-100 p-8 font-sans">
      {/* HEADER */}
      <div className="flex justify-between items-center mb-8 pb-4 border-b border-gray-800">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/dashboard')}
            className="p-2 bg-[#161b22] border border-gray-800 hover:border-indigo-500/50 rounded-xl text-gray-400 hover:text-white transition-all flex items-center gap-2 text-xs font-semibold"
          >
            <ArrowLeft className="w-4 h-4" />
            Dashboard'a Dön
          </button>
          <div>
            <h1 className="text-2xl font-bold text-white flex items-center gap-2">
              <Users className="w-6 h-6 text-indigo-400" />
              Workspace Üyeleri
            </h1>
            <p className="text-xs text-gray-400 mt-1">
              Bu çalışma alanındaki tüm geliştiriciler ve yöneticiler
            </p>
          </div>
        </div>

        <span className="text-xs font-mono bg-[#161b22] text-indigo-400 border border-indigo-500/30 px-3 py-1.5 rounded-full font-bold">
          Toplam: {members.length} Üye
        </span>
      </div>

      {/* ÜYE KARTLARI GRİDİ */}
      {loading ? (
        <div className="flex justify-center items-center py-20 text-gray-400 gap-2">
          <RefreshCw className="w-5 h-5 animate-spin text-indigo-400" />
          <span>Ekip üyeleri yükleniyor...</span>
        </div>
      ) : members.length === 0 ? (
        <div className="text-center py-20 text-gray-500 text-sm">
          Bu workspace'e bağlı üye bulunamadı.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {members.map((member) => (
            <div
              key={member.id}
              className="bg-[#161b22] border border-gray-800 hover:border-indigo-500/40 p-6 rounded-2xl shadow-lg transition-all flex flex-col justify-between group"
            >
              <div>
                {/* Profil Fotoğrafı / Avatar */}
                <div className="flex items-center gap-4 mb-4">
                  <div className="w-14 h-14 rounded-2xl bg-indigo-950 border border-indigo-500/40 flex items-center justify-center overflow-hidden shrink-0 shadow-inner group-hover:scale-105 transition-transform">
                    {member.profile_picture ? (
                      <img
                        src={member.profile_picture}
                        alt={member.username}
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <span className="text-indigo-300 font-extrabold text-xl uppercase">
                        {member.username ? member.username.charAt(0) : <User className="w-6 h-6 text-indigo-400" />}
                      </span>
                    )}
                  </div>

                  <div className="truncate">
                    <h3 className="font-bold text-base text-white truncate group-hover:text-indigo-300 transition-colors">
                      {member.username || 'İsimsiz Kullanıcı'}
                    </h3>
                    <span className="inline-block mt-1 px-2 py-0.5 text-[10px] bg-indigo-600/20 text-indigo-300 rounded-md border border-indigo-500/30 font-semibold uppercase">
                      {member.appRole || member.role_key || 'DEVELOPER'}
                    </span>
                  </div>
                </div>

                {/* Bilgi Detayları */}
                <div className="space-y-2 pt-3 border-t border-gray-800/80 text-xs">
                  <div className="flex items-center gap-2 text-gray-400">
                    <Mail className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                    <span className="truncate">{member.email || 'E-posta Yok'}</span>
                  </div>
                  <div className="flex items-center gap-2 text-gray-400 font-mono">
                    <Shield className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                    <span>ID: {member.id}</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}