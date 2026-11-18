import React from 'react';

const PALETTE = [
  '#3730a3',
  '#0f766e',
  '#1d4ed8',
  '#6d28d9',
  '#0369a1',
  '#b45309',
  '#15803d',
  '#9f1239'
];

function getInitials(name) {
  if (!name || typeof name !== 'string') return 'U';
  const clean = name.trim();
  const parts = clean.split(/[\s._-]+/).filter(Boolean);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  }
  return clean.slice(0, 2).toUpperCase();
}

function getColor(seed) {
  if (!seed) return PALETTE[0];
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash * 31 + seed.charCodeAt(i)) % 997;
  }
  return PALETTE[Math.abs(hash) % PALETTE.length];
}

const Avatar = ({ name, size = 34 }) => {
  const initials = getInitials(name);
  const bg = getColor(name || 'EPAMS');

  return (
    <span
      className="ep-avatar"
      aria-hidden="true"
      style={{
        width: `${size}px`,
        height: `${size}px`,
        fontSize: `${Math.max(11, Math.round(size * 0.38))}px`,
        backgroundColor: bg
      }}
    >
      {initials}
    </span>
  );
};

export default Avatar;
