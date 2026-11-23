import React from 'react';

/**
 * EMPORA Enterprise Brand Logo & Monogram Emblem
 * Modern geometric isometric hexagon with interconnected workflow nodes and stylized 'E' core.
 */
export const EmporaLogoMark = ({ size = 36, glow = true, className = '' }) => {
  const gradientId = `empora-grad-${size}`;
  const glowId = `empora-glow-${size}`;

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 48 48"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`ep-logo-mark ${className}`}
      style={{
        flexShrink: 0,
        filter: glow ? 'drop-shadow(0 4px 14px rgba(79, 70, 229, 0.45))' : 'none',
        transition: 'transform 0.3s cubic-bezier(0.16, 1, 0.3, 1), filter 0.3s ease'
      }}
      aria-hidden="true"
    >
      <defs>
        <linearGradient id={gradientId} x1="4" y1="4" x2="44" y2="44" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#6366f1" />
          <stop offset="50%" stopColor="#4f46e5" />
          <stop offset="100%" stopColor="#06b6d4" />
        </linearGradient>
        <linearGradient id={`${gradientId}-accent`} x1="12" y1="12" x2="36" y2="36" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#a855f7" />
          <stop offset="100%" stopColor="#3b82f6" />
        </linearGradient>
        <filter id={glowId} x="-20%" y="-20%" width="140%" height="140%">
          <feDropShadow dx="0" dy="2" stdDeviation="3" floodColor="#4f46e5" floodOpacity="0.5" />
        </filter>
      </defs>

      {/* Outer Hexagonal Shield Frame */}
      <path
        d="M24 4L42 14.5V33.5L24 44L6 33.5V14.5L24 4Z"
        fill={`url(#${gradientId})`}
        stroke="rgba(255, 255, 255, 0.25)"
        strokeWidth="1.5"
      />

      {/* Inner Translucent Facet */}
      <path
        d="M24 8L38 16.2V31.8L24 40L10 31.8V16.2L24 8Z"
        fill="rgba(15, 23, 42, 0.35)"
        stroke="rgba(255, 255, 255, 0.12)"
        strokeWidth="1"
      />

      {/* Stylized Modern 'E' Monogram & Circuit Nodes */}
      {/* Top Bar of E */}
      <path
        d="M16 18H32C33.1 18 34 18.9 34 20C34 21.1 33.1 22 32 22H20V23"
        stroke="#ffffff"
        strokeWidth="3.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* Middle Bar with glowing pulse dot */}
      <path
        d="M16 24H27C27.8 24 28.5 24.7 28.5 25.5C28.5 26.3 27.8 27 27 27H20"
        stroke="#ffffff"
        strokeWidth="3.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* Bottom Bar of E */}
      <path
        d="M16 30H32C33.1 30 34 30.9 34 32C34 33.1 33.1 34 32 34H16C14.9 34 14 33.1 14 32V20C14 18.9 14.9 18 16 18Z"
        stroke="#ffffff"
        strokeWidth="3.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />

      {/* Glowing Energy Core Node */}
      <circle cx="34" cy="20" r="2.2" fill="#38bdf8" />
      <circle cx="27" cy="25.5" r="2" fill="#a78bfa" />
      <circle cx="34" cy="32" r="2.2" fill="#34d399" />
    </svg>
  );
};

export const EmporaLogo = ({
  size = 'md',
  showSub = true,
  glow = true,
  className = ''
}) => {
  const pixelSize = size === 'sm' ? 28 : size === 'lg' ? 48 : size === 'xl' ? 60 : 36;
  const titleSize = size === 'sm' ? '14px' : size === 'lg' ? '22px' : size === 'xl' ? '28px' : '17px';
  const subSize = size === 'sm' ? '10px' : size === 'lg' ? '12px' : size === 'xl' ? '13px' : '11px';

  return (
    <div
      className={`ep-logo-lockup ${className}`}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: size === 'sm' ? '8px' : size === 'lg' || size === 'xl' ? '14px' : '10px',
        textDecoration: 'none'
      }}
    >
      <EmporaLogoMark size={pixelSize} glow={glow} />
      <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.15 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span
            className="ep-brand-title"
            style={{
              fontSize: titleSize,
              fontWeight: 900,
              letterSpacing: '-0.02em',
              background: 'linear-gradient(135deg, var(--ep-text-primary, #0f172a) 40%, #4f46e5 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'currentColor'
            }}
          >
            EMPORA
          </span>
          <span
            style={{
              fontSize: '9.5px',
              fontWeight: 800,
              padding: '2px 6px',
              borderRadius: '6px',
              letterSpacing: '0.06em',
              textTransform: 'uppercase',
              background: 'rgba(79, 70, 229, 0.12)',
              color: 'var(--ep-primary, #4f46e5)',
              border: '1px solid rgba(79, 70, 229, 0.25)'
            }}
          >
            PRO
          </span>
        </div>
        {showSub && (
          <span
            className="ep-brand-sub"
            style={{
              fontSize: subSize,
              color: 'var(--ep-text-muted, #64748b)',
              fontWeight: 500,
              letterSpacing: '0.01em',
              marginTop: '2px'
            }}
          >
            Employee Services &amp; Workflow
          </span>
        )}
      </div>
    </div>
  );
};

export default EmporaLogo;
