/**
 * [A05: Injection & XSS Prevention]
 * Frontend Security Utilities for EMPORA React Single Page Application.
 */

const ALLOWED_PROTOCOLS = new Set(['http:', 'https:', 'mailto:', 'tel:']);

/**
 * Validates whether a given URL is safe to render in <a href={url}> or window.location.
 * Strictly prevents DOM-based XSS via 'javascript:', 'data:', 'vbscript:' pseudo-protocols.
 *
 * @param {string} url - Target URL to inspect
 * @param {string} fallback - Safe fallback URL (defaults to '#')
 * @returns {string} Sanitized, validated safe URL
 */
export function getSafeUrl(url, fallback = '#') {
  if (!url || typeof url !== 'string') {
    return fallback;
  }

  const trimmed = url.trim();

  // Allow relative paths starting with /
  if (trimmed.startsWith('/') && !trimmed.startsWith('//')) {
    return trimmed;
  }

  try {
    const parsed = new URL(trimmed, window.location.origin);
    if (ALLOWED_PROTOCOLS.has(parsed.protocol)) {
      return trimmed;
    }
    // Blocked protocol
    console.warn('[A05: XSS Prevention] Blocked dangerous URL protocol:', parsed.protocol);
    return fallback;
  } catch (e) {
    // Malformed URL
    return fallback;
  }
}

/**
 * Validates whether a given URL is safe without returning a fallback.
 *
 * @param {string} url
 * @returns {boolean}
 */
export function isSafeUrl(url) {
  if (!url || typeof url !== 'string') return false;
  const trimmed = url.trim();
  if (trimmed.startsWith('/') && !trimmed.startsWith('//')) return true;
  try {
    const parsed = new URL(trimmed, window.location.origin);
    return ALLOWED_PROTOCOLS.has(parsed.protocol);
  } catch {
    return false;
  }
}

/**
 * Sanitizes user input string by stripping potential dangerous control characters.
 *
 * @param {string} input
 * @returns {string}
 */
export function sanitizeInput(input) {
  if (typeof input !== 'string') return '';
  return input.replace(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/g, '').trim();
}
