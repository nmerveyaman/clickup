import React, { useState } from 'react';

interface UserAvatarProps {
  username?: string;
  profilePicture?: string | null;
  size?: number; // örn: 32px
}

export const UserAvatar: React.FC<UserAvatarProps> = ({ username, profilePicture, size = 32 }) => {
  const [imgError, setImgError] = useState(false);

  // Kullanıcı adının baş harflerini al (Örn: "Merve Yaman" -> "MY")
  const getInitials = (name?: string) => {
    if (!name) return '?';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  };

  // Profil fotoğrafı var ve yükleme hatası almadıysa resmi göster
  if (profilePicture && !imgError) {
    return (
      <img
        src={profilePicture}
        alt={username || 'User Avatar'}
        onError={() => setImgError(true)} // Link kırıksa fallback'e düş
        className="rounded-full object-cover"
        style={{ width: `${size}px`, height: `${size}px` }}
      />
    );
  }

  // Fotoğraf yoksa veya yüklenemediyse renkli yuvarlak içinde baş harfleri göster
  return (
    <div
      className="flex items-center justify-center rounded-full bg-indigo-600 text-white font-semibold text-xs shadow-sm"
      style={{ width: `${size}px`, height: `${size}px` }}
      title={username}
    >
      {getInitials(username)}
    </div>
  );
};