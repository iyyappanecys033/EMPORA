import React, { createContext, useContext, useState, useEffect, useCallback, useMemo } from 'react';

const STORAGE_KEY = 'epams.preferences';

const DEFAULT_PREFERENCES = {
  theme: 'system', // 'light' | 'dark' | 'system'
  density: 'comfortable', // 'comfortable' | 'compact'
  reducedMotion: 'system', // 'system' | 'on' | 'off'
  fontScale: 'standard', // 'standard' | 'large'
  sidebarCollapsed: false
};

function readStoredPreferences() {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_PREFERENCES;
    const parsed = JSON.parse(raw);
    return {
      theme: ['light', 'dark', 'system'].includes(parsed.theme) ? parsed.theme : 'system',
      density: ['comfortable', 'compact'].includes(parsed.density) ? parsed.density : 'comfortable',
      reducedMotion: ['system', 'on', 'off'].includes(parsed.reducedMotion) ? parsed.reducedMotion : 'system',
      fontScale: ['standard', 'large'].includes(parsed.fontScale) ? parsed.fontScale : 'standard',
      sidebarCollapsed: Boolean(parsed.sidebarCollapsed)
    };
  } catch {
    return DEFAULT_PREFERENCES;
  }
}

function getSystemDark() {
  if (typeof window === 'undefined' || !window.matchMedia) return false;
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

function getSystemReducedMotion() {
  if (typeof window === 'undefined' || !window.matchMedia) return false;
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

const PreferencesContext = createContext({
  ...DEFAULT_PREFERENCES,
  resolvedTheme: 'light',
  effectiveReducedMotion: false,
  updatePreference: () => {},
  resetPreferences: () => {},
  toggleSidebarCollapsed: () => {}
});

export const PreferencesProvider = ({ children }) => {
  const [prefs, setPrefs] = useState(readStoredPreferences);
  const [systemDark, setSystemDark] = useState(getSystemDark);
  const [systemReducedMotion, setSystemReducedMotion] = useState(getSystemReducedMotion);

  useEffect(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return undefined;
    const darkMq = window.matchMedia('(prefers-color-scheme: dark)');
    const motionMq = window.matchMedia('(prefers-reduced-motion: reduce)');

    const onDarkChange = (e) => setSystemDark(e.matches);
    const onMotionChange = (e) => setSystemReducedMotion(e.matches);

    if (darkMq.addEventListener) {
      darkMq.addEventListener('change', onDarkChange);
      motionMq.addEventListener('change', onMotionChange);
    } else if (darkMq.addListener) {
      darkMq.addListener(onDarkChange);
      motionMq.addListener(onMotionChange);
    }

    return () => {
      if (darkMq.removeEventListener) {
        darkMq.removeEventListener('change', onDarkChange);
        motionMq.removeEventListener('change', onMotionChange);
      } else if (darkMq.removeListener) {
        darkMq.removeListener(onDarkChange);
        motionMq.removeListener(onMotionChange);
      }
    };
  }, []);

  const resolvedTheme =
    prefs.theme === 'dark' || (prefs.theme === 'system' && systemDark) ? 'dark' : 'light';

  const effectiveReducedMotion =
    prefs.reducedMotion === 'on' || (prefs.reducedMotion === 'system' && systemReducedMotion);

  useEffect(() => {
    const root = document.documentElement;
    root.setAttribute('data-theme', resolvedTheme);
    root.setAttribute('data-density', prefs.density);
    root.setAttribute('data-font-scale', prefs.fontScale);
    root.setAttribute('data-reduced-motion', effectiveReducedMotion ? 'true' : 'false');
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs));
    } catch {
      // Ignore storage quota / private mode failures safely
    }
  }, [prefs, resolvedTheme, effectiveReducedMotion]);

  const updatePreference = useCallback((key, value) => {
    setPrefs((prev) => ({ ...prev, [key]: value }));
  }, []);

  const toggleSidebarCollapsed = useCallback(() => {
    setPrefs((prev) => ({ ...prev, sidebarCollapsed: !prev.sidebarCollapsed }));
  }, []);

  const resetPreferences = useCallback(() => {
    setPrefs(DEFAULT_PREFERENCES);
  }, []);

  const value = useMemo(
    () => ({
      ...prefs,
      resolvedTheme,
      effectiveReducedMotion,
      updatePreference,
      resetPreferences,
      toggleSidebarCollapsed
    }),
    [prefs, resolvedTheme, effectiveReducedMotion, updatePreference, resetPreferences, toggleSidebarCollapsed]
  );

  return <PreferencesContext.Provider value={value}>{children}</PreferencesContext.Provider>;
};

export const usePreferences = () => useContext(PreferencesContext);
export default PreferencesContext;
