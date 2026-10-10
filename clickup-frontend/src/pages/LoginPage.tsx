import { Layers, ArrowRight, CheckCircle2 } from 'lucide-react';
import { API_URLS } from '../constants/apiUrls';

interface LoginPageProps {
  onLoginSuccess: (userData?: { userId: number; username: string; role: string }) => void;
}

export default function LoginPage({ onLoginSuccess }: LoginPageProps) {
  const handleLoginWithClickUp = () => {
    // Spring Boot auth başlatma endpoint'ine yönlendirir
    window.location.href = API_URLS.AUTH.CLICKUP_LOGIN;
  };

  return (
    <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl p-8 max-w-md w-full shadow-2xl text-center">
        <div className="w-16 h-16 bg-indigo-600/20 border border-indigo-500/30 rounded-2xl flex items-center justify-center mx-auto mb-6">
          <Layers className="w-8 h-8 text-indigo-400" />
        </div>
        <h1 className="text-2xl font-bold text-white mb-2">ClickUp Analytics</h1>
        <p className="text-slate-400 text-sm mb-8">
          Sprint metriklerini, darboğazları ve geliştirici Gantt şemalarını görüntülemek için çalışma alanınızı bağlayın.
        </p>

        <div className="text-left space-y-3 mb-8 bg-slate-900/50 p-4 rounded-xl border border-slate-700/50">
          <div className="flex items-center gap-3 text-sm text-slate-300">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>Sprint tamamlanma ve Scope Creep analizi</span>
          </div>
          <div className="flex items-center gap-3 text-sm text-slate-300">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>Görev bazlı kolon bekleme süreleri</span>
          </div>
          <div className="flex items-center gap-3 text-sm text-slate-300">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>Geliştirici bazlı çakışmasız Gantt çizelgesi</span>
          </div>
        </div>

        <button
          onClick={handleLoginWithClickUp}
          className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-medium py-3 px-4 rounded-xl flex items-center justify-center gap-2 transition-all shadow-lg shadow-indigo-600/30 cursor-pointer"
        >
          <span>ClickUp Workspace ile Bağlan</span>
          <ArrowRight className="w-4 h-4" />
        </button>

        <button
          onClick={() => onLoginSuccess({ userId: 1, username: 'Merve Yaman', role: 'ADMIN' })}
          className="mt-3 text-xs text-slate-500 hover:text-slate-300 underline cursor-pointer"
        >
          (Backend'i bekletmeden direkt Dashboard'u test et)
        </button>
      </div>
    </div>
  );
}