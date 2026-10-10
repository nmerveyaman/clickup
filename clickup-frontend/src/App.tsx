import { useState, useEffect } from 'react';
  
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';

interface UserSession {
  userId: number;
  username: string;
  role: string;
}

export default function App() {
  const [user, setUser] = useState<UserSession | null>(null);

  // Spring Boot'tan gelen başarı parametrelerini veya localStorage oturumunu kontrol et
  useEffect(() => {
    const queryParams = new URLSearchParams(window.location.search);
    const userId = queryParams.get('userId');
    const username = queryParams.get('username');
    const role = queryParams.get('role');

    if (userId && username) {
      const sessionData: UserSession = {
        userId: Number(userId),
        username,
        role: role || 'USER',
      };
      setUser(sessionData);
      localStorage.setItem('user_session', JSON.stringify(sessionData));

      // URL'deki query parametrelerini temizle (Temiz görünüm için)
      window.history.replaceState({}, document.title, window.location.pathname);
    } else {
      // Daha önceden giriş yapılmışsa localStorage'dan al
      const savedSession = localStorage.getItem('user_session');
      if (savedSession) {
        try {
          setUser(JSON.parse(savedSession));
        } catch {
          localStorage.removeItem('user_session');
        }
      }
    }
  }, []);

  const handleLogout = () => {
    setUser(null);
    localStorage.removeItem('user_session');
  };

  if (!user) {
    return <LoginPage onLoginSuccess={(userData) => setUser(userData || { userId: 1, username: 'Merve', role: 'ADMIN' })} />;
  }

  return <DashboardPage user={user} onLogout={handleLogout} />;
}