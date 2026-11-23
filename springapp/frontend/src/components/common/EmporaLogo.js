import React from 'react';

/**
 * 3D Beveled Sapphire Metallic "E" Monogram Logo
 * Precision chiseled multi-faceted metallic emblem inspired by the 3D EmpOpera brandmark.
 */
export const EmporaLogoMark = ({ size = 36, glow = true, className = '' }) => {
  const gradPrefix = `ep-3d-${size}`;

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`ep-3d-logo-mark ${className}`}
      style={{
        flexShrink: 0,
        filter: glow ? 'drop-shadow(0 6px 18px rgba(37, 99, 235, 0.55)) drop-shadow(0 2px 6px rgba(56, 189, 248, 0.4))' : 'none',
        transition: 'transform 0.25s cubic-bezier(0.16, 1, 0.3, 1), filter 0.25s ease'
      }}
      aria-hidden="true"
    >
      <defs>
        {/* Top-facing Specular Highlight Gradient (Bright Ice Blue) */}
        <linearGradient id={`${gradPrefix}-top-light`} x1="12" y1="10" x2="86" y2="25" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#93c5fd" />
          <stop offset="45%" stopColor="#e0f2fe" />
          <stop offset="70%" stopColor="#60a5fa" />
          <stop offset="100%" stopColor="#3b82f6" />
        </linearGradient>

        {/* Vertical Left Facet (Metallic Cobalt) */}
        <linearGradient id={`${gradPrefix}-left-facet`} x1="12" y1="10" x2="24" y2="90" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#60a5fa" />
          <stop offset="30%" stopColor="#2563eb" />
          <stop offset="70%" stopColor="#1d4ed8" />
          <stop offset="100%" stopColor="#1e3a8a" />
        </linearGradient>

        {/* Downward / Inward Shadow Facets (Deep Sapphire Navy) */}
        <linearGradient id={`${gradPrefix}-deep-shadow`} x1="24" y1="20" x2="86" y2="35" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#172554" />
          <stop offset="50%" stopColor="#1e3a8a" />
          <stop offset="100%" stopColor="#0f172a" />
        </linearGradient>

        {/* Middle Bar Top Highlight */}
        <linearGradient id={`${gradPrefix}-mid-light`} x1="24" y1="44" x2="70" y2="50" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#38bdf8" />
          <stop offset="50%" stopColor="#93c5fd" />
          <stop offset="100%" stopColor="#2563eb" />
        </linearGradient>

        {/* Middle Bar Downward Shadow */}
        <linearGradient id={`${gradPrefix}-mid-shadow`} x1="24" y1="50" x2="70" y2="56" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#1e3a8a" />
          <stop offset="100%" stopColor="#0f2b6b" />
        </linearGradient>

        {/* Bottom Bar Upper Highlight */}
        <linearGradient id={`${gradPrefix}-bot-light`} x1="24" y1="68" x2="86" y2="79" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#3b82f6" />
          <stop offset="60%" stopColor="#60a5fa" />
          <stop offset="100%" stopColor="#1d4ed8" />
        </linearGradient>

        {/* Bottom Bar Bottom-most Shadow */}
        <linearGradient id={`${gradPrefix}-bot-shadow`} x1="12" y1="80" x2="86" y2="90" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#091b42" />
          <stop offset="60%" stopColor="#172554" />
          <stop offset="100%" stopColor="#0b1739" />
        </linearGradient>

        {/* Right Tip Bevels */}
        <linearGradient id={`${gradPrefix}-tip-facet`} x1="76" y1="10" x2="86" y2="32" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#1d4ed8" />
          <stop offset="100%" stopColor="#0f2b6b" />
        </linearGradient>

        <linearGradient id={`${gradPrefix}-ridge-glow`} x1="12" y1="10" x2="86" y2="90" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#ffffff" stopOpacity="0.9" />
          <stop offset="50%" stopColor="#bae6fd" stopOpacity="0.8" />
          <stop offset="100%" stopColor="#60a5fa" stopOpacity="0.6" />
        </linearGradient>
      </defs>

      {/* ============================================================== */}
      {/* 3D FACETS OF THE CHISELED METALLIC SAPPHIRE "E"               */}
      {/* ============================================================== */}

      {/* 1. TOP ARM - Upper Specular Highlight Facet */}
      <path
        d="M12 10 L86 10 L76 21 L24 21 Z"
        fill={`url(#${gradPrefix}-top-light)`}
      />

      {/* 2. TOP ARM - Right Bevel Edge */}
      <path
        d="M86 10 L86 32 L76 21 Z"
        fill={`url(#${gradPrefix}-tip-facet)`}
      />

      {/* 3. TOP ARM - Lower Downward Shadow Facet */}
      <path
        d="M24 21 L76 21 L86 32 L36 32 Z"
        fill={`url(#${gradPrefix}-deep-shadow)`}
      />

      {/* 4. VERTICAL SPINE - Front/Left Major Metallic Facet */}
      <path
        d="M12 10 L24 21 L24 79 L12 90 Z"
        fill={`url(#${gradPrefix}-left-facet)`}
      />

      {/* 5. VERTICAL SPINE - Top Inward Inner Bevel */}
      <path
        d="M24 21 L36 32 L36 44 L24 50 Z"
        fill="#0f2b6b"
      />

      {/* 6. MIDDLE ARM - Upper Highlight Facet */}
      <path
        d="M24 50 L36 44 L70 44 L62 50 Z"
        fill={`url(#${gradPrefix}-mid-light)`}
      />

      {/* 7. MIDDLE ARM - Chiseled Wedge Tip */}
      <path
        d="M70 44 L70 56 L62 50 Z"
        fill="#1e40af"
      />

      {/* 8. MIDDLE ARM - Lower Shadow Facet */}
      <path
        d="M24 50 L62 50 L70 56 L36 56 Z"
        fill={`url(#${gradPrefix}-mid-shadow)`}
      />

      {/* 9. VERTICAL SPINE - Bottom Inward Inner Bevel */}
      <path
        d="M24 50 L36 56 L36 68 L24 79 Z"
        fill="#0f2b6b"
      />

      {/* 10. BOTTOM ARM - Upper Highlight Facet */}
      <path
        d="M24 79 L36 68 L86 68 L76 79 Z"
        fill={`url(#${gradPrefix}-bot-light)`}
      />

      {/* 11. BOTTOM ARM - Right Bevel Edge */}
      <path
        d="M86 68 L86 90 L76 79 Z"
        fill="#172554"
      />

      {/* 12. BOTTOM ARM - Lower-most Oceanic Navy Base Shadow */}
      <path
        d="M12 90 L24 79 L76 79 L86 90 Z"
        fill={`url(#${gradPrefix}-bot-shadow)`}
      />

      {/* ============================================================== */}
      {/* SPECULAR CHISEL RIDGE HIGHLIGHT LINES (Precision 3D Edge Cut)  */}
      {/* ============================================================== */}
      {/* Top ridge line */}
      <line x1="24" y1="21" x2="76" y2="21" stroke={`url(#${gradPrefix}-ridge-glow)`} strokeWidth="1.2" strokeLinecap="round" />
      {/* Vertical spine ridge line */}
      <line x1="24" y1="21" x2="24" y2="79" stroke={`url(#${gradPrefix}-ridge-glow)`} strokeWidth="1.4" strokeLinecap="round" />
      {/* Middle arm ridge line */}
      <line x1="24" y1="50" x2="62" y2="50" stroke={`url(#${gradPrefix}-ridge-glow)`} strokeWidth="1.2" strokeLinecap="round" />
      {/* Bottom arm ridge line */}
      <line x1="24" y1="79" x2="76" y2="79" stroke={`url(#${gradPrefix}-ridge-glow)`} strokeWidth="1.2" strokeLinecap="round" />

      {/* Outer rim chamfer accents for ultra-crisp metallic finish */}
      <line x1="12" y1="10" x2="86" y2="10" stroke="#bae6fd" strokeWidth="0.8" strokeOpacity="0.8" />
      <line x1="12" y1="10" x2="12" y2="90" stroke="#93c5fd" strokeWidth="0.8" strokeOpacity="0.6" />
    </svg>
  );
};

export const EmporaLogo = ({
  size = 'md',
  showSub = true,
  glow = true,
  className = ''
}) => {
  const pixelSize = size === 'sm' ? 26 : size === 'lg' ? 44 : size === 'xl' ? 56 : 34;
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
              background: 'linear-gradient(135deg, var(--ep-text-primary, #0f172a) 40%, #2563eb 100%)',
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
              background: 'rgba(37, 99, 235, 0.12)',
              color: 'var(--ep-primary, #2563eb)',
              border: '1px solid rgba(37, 99, 235, 0.28)'
            }}
          >
            ENTERPRISE
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
