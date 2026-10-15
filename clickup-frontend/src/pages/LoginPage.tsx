import React from 'react';
import { API_URLS } from '../constants/apiUrls';

export default function LoginPage() {
  const handleLoginWithClickUp = () => {
    // Spring Boot üzerindeki /login endpoint'ine yönlendiriyoruz.
    // Backend 302 Redirect atarak tarayıcıyı ClickUp'ın OAuth sayfasına fırlatacak.
    window.location.href = API_URLS.LOGIN;
  };

  return (
    <div className="flex h-screen items-center justify-center bg-slate-900 text-white font-sans">
      <div className="text-center p-8 bg-slate-800 rounded-2xl border border-slate-700 shadow-2xl max-w-sm w-full">
        <h1 className="text-xl font-bold mb-2">ClickUp Analytics</h1>
        <p className="text-slate-400 text-xs mb-6">
          Sprint metriklerini ve geliştirici eforlarını incelemek için giriş yapın.
        </p>
        <button
          onClick={handleLoginWithClickUp}
          className="w-full py-3 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-500 font-semibold text-xs text-white transition-all shadow-lg shadow-indigo-600/30"
        >
          ClickUp Workspace ile Bağlan
        </button>
      </div>
    </div>
  );
}