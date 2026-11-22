import React from 'react';
import { usePreferences } from '../../services/PreferencesContext';
import { useToast } from '../../services/ToastContext';
import { SunIcon, MoonIcon, MonitorIcon } from '../common/Icons';

const PreferencesPage = () => {
  const {
    theme,
    resolvedTheme,
    density,
    reducedMotion,
    effectiveReducedMotion,
    fontScale,
    sidebarCollapsed,
    updatePreference,
    resetPreferences
  } = usePreferences();
  const { showToast } = useToast();

  const handleChange = (key, value, label) => {
    updatePreference(key, value);
    showToast({
      type: 'success',
      title: 'Preference Updated',
      message: `${label} applied immediately.`
    });
  };

  return (
    <div style={{ maxWidth: '860px' }}>
      <div className="ep-page-header">
        <div>
          <h1 className="ep-page-title">Preferences &amp; Accessibility</h1>
          <p className="ep-page-subtitle">
            Personalize your EMPORA workspace theme, density, typography scale, and motion settings.
          </p>
        </div>
        <button
          type="button"
          className="ep-btn ep-btn--secondary"
          onClick={() => {
            resetPreferences();
            showToast({ type: 'info', title: 'Preferences Reset', message: 'Restored default workspace settings.' });
          }}
        >
          Reset to Defaults
        </button>
      </div>

      {/* Honest Device-Local Persistence Notice */}
      <div
        role="note"
        style={{
          padding: '12px 16px',
          borderRadius: 'var(--ep-radius-md)',
          background: 'var(--ep-info-bg)',
          border: '1px solid var(--ep-info-border)',
          color: 'var(--ep-info-text)',
          fontSize: '12px',
          marginBottom: '20px',
          lineHeight: 1.5
        }}
      >
        <strong>Browser-Local Persistence:</strong> Personalization preferences are stored securely in this browser&apos;s local storage (<code>empora.preferences</code>) and are specific to this device. No backend preference API is currently provisioned.
      </div>

      <div style={{ display: 'grid', gap: '18px' }}>
        {/* Theme Selection */}
        <section className="ep-card" aria-labelledby="pref-theme-heading">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px', marginBottom: '12px' }}>
            <div>
              <h2 id="pref-theme-heading" style={{ margin: 0, fontSize: '16px', fontWeight: 700 }}>
                Appearance &amp; Theme
              </h2>
              <p style={{ margin: '3px 0 0', fontSize: '13px', color: 'var(--ep-text-muted)' }}>
                Choose Light, Dark, or follow your operating system automatically. Active theme: <strong>{resolvedTheme}</strong>.
              </p>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))', gap: '12px' }} role="radiogroup" aria-label="Theme mode">
            {[
              { id: 'light', title: 'Light', desc: 'Crisp high-contrast daylight workspace', Icon: SunIcon },
              { id: 'dark', title: 'Dark', desc: 'Tonal dark surfaces for low-light focus', Icon: MoonIcon },
              { id: 'system', title: 'System', desc: 'Syncs with your OS appearance setting', Icon: MonitorIcon }
            ].map(({ id, title, desc, Icon }) => {
              const selected = theme === id;
              return (
                <button
                  key={id}
                  type="button"
                  role="radio"
                  aria-checked={selected}
                  onClick={() => handleChange('theme', id, `Theme (${title})`)}
                  style={{
                    textAlign: 'left',
                    padding: '14px',
                    borderRadius: 'var(--ep-radius-md)',
                    border: selected ? '2px solid var(--ep-primary)' : '1px solid var(--ep-border)',
                    background: selected ? 'var(--ep-primary-subtle)' : 'var(--ep-bg-subtle)',
                    color: 'var(--ep-text-primary)',
                    cursor: 'pointer'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 700, fontSize: '14px' }}>
                    <Icon size={17} /> {title}
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--ep-text-muted)', marginTop: '6px' }}>{desc}</div>
                </button>
              );
            })}
          </div>
        </section>

        {/* Density & Layout */}
        <section className="ep-card" aria-labelledby="pref-density-heading">
          <h2 id="pref-density-heading" style={{ margin: '0 0 4px', fontSize: '16px', fontWeight: 700 }}>
            Layout &amp; Table Density
          </h2>
          <p style={{ margin: '0 0 14px', fontSize: '13px', color: 'var(--ep-text-muted)' }}>
            Adjust spacing across tables, queues, and workspace cards.
          </p>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', alignItems: 'center', justifyContent: 'space-between' }}>
            <div className="ep-pill-group" role="radiogroup" aria-label="Interface density">
              {[
                { id: 'comfortable', label: 'Comfortable (Default)' },
                { id: 'compact', label: 'Compact (High Density)' }
              ].map((opt) => (
                <button
                  key={opt.id}
                  type="button"
                  role="radio"
                  aria-checked={density === opt.id}
                  onClick={() => handleChange('density', opt.id, `Density (${opt.label})`)}
                  className={`ep-pill ${density === opt.id ? 'ep-pill--active' : ''}`}
                  style={{ padding: '7px 14px' }}
                >
                  {opt.label}
                </button>
              ))}
            </div>

            <label style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '13px' }}>
              <input
                type="checkbox"
                checked={sidebarCollapsed}
                onChange={(e) => handleChange('sidebarCollapsed', e.target.checked, 'Sidebar rail layout')}
              />
              Start with compact icon sidebar on desktop
            </label>
          </div>
        </section>

        {/* Accessibility & Motion */}
        <section className="ep-card" aria-labelledby="pref-a11y-heading">
          <h2 id="pref-a11y-heading" style={{ margin: '0 0 4px', fontSize: '16px', fontWeight: 700 }}>
            Accessibility &amp; Motion
          </h2>
          <p style={{ margin: '0 0 14px', fontSize: '13px', color: 'var(--ep-text-muted)' }}>
            Control animations and text scaling. Reduced motion currently: <strong>{effectiveReducedMotion ? 'Active' : 'Standard'}</strong>.
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
            <div>
              <div className="ep-label">Motion Preference</div>
              <div className="ep-pill-group" role="radiogroup" aria-label="Motion preference">
                {[
                  { id: 'system', label: 'Follow OS' },
                  { id: 'on', label: 'Reduce Motion' },
                  { id: 'off', label: 'Standard Motion' }
                ].map((opt) => (
                  <button
                    key={opt.id}
                    type="button"
                    role="radio"
                    aria-checked={reducedMotion === opt.id}
                    onClick={() => handleChange('reducedMotion', opt.id, `Motion (${opt.label})`)}
                    className={`ep-pill ${reducedMotion === opt.id ? 'ep-pill--active' : ''}`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <div className="ep-label">Text Size</div>
              <div className="ep-pill-group" role="radiogroup" aria-label="Text size">
                {[
                  { id: 'standard', label: 'Standard (100%)' },
                  { id: 'large', label: 'Large (112.5%)' }
                ].map((opt) => (
                  <button
                    key={opt.id}
                    type="button"
                    role="radio"
                    aria-checked={fontScale === opt.id}
                    onClick={() => handleChange('fontScale', opt.id, `Text Size (${opt.label})`)}
                    className={`ep-pill ${fontScale === opt.id ? 'ep-pill--active' : ''}`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
};

export default PreferencesPage;
