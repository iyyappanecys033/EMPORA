import { getSafeUrl, isSafeUrl, sanitizeInput } from './securityUtils';

describe('[A05: Injection & XSS Prevention] securityUtils Tests', () => {
  describe('getSafeUrl and isSafeUrl', () => {
    test('allows relative paths starting with /', () => {
      expect(getSafeUrl('/dashboard')).toBe('/dashboard');
      expect(getSafeUrl('/tickets/101?status=OPEN')).toBe('/tickets/101?status=OPEN');
      expect(isSafeUrl('/catalog')).toBe(true);
    });

    test('allows trusted http and https protocols', () => {
      expect(getSafeUrl('https://app.empora.internal/docs')).toBe('https://app.empora.internal/docs');
      expect(getSafeUrl('http://localhost:3000')).toBe('http://localhost:3000');
      expect(isSafeUrl('https://secure.empora.com')).toBe(true);
    });

    test('strictly blocks dangerous javascript: pseudo-protocol', () => {
      const malicious = 'javascript:alert(document.cookie)';
      expect(getSafeUrl(malicious)).toBe('#');
      expect(getSafeUrl(malicious, '/safe-fallback')).toBe('/safe-fallback');
      expect(isSafeUrl(malicious)).toBe(false);
    });

    test('strictly blocks data: and vbscript: URIs', () => {
      expect(getSafeUrl('data:text/html,<script>alert(1)</script>')).toBe('#');
      expect(getSafeUrl('vbscript:msgbox(1)')).toBe('#');
      expect(isSafeUrl('data:image/svg+xml;base64,PHN2Zw==')).toBe(false);
    });

    test('handles null, undefined, empty, and malformed inputs gracefully', () => {
      expect(getSafeUrl(null)).toBe('#');
      expect(getSafeUrl(undefined)).toBe('#');
      expect(getSafeUrl('')).toBe('#');
      expect(isSafeUrl(null)).toBe(false);
      expect(isSafeUrl('')).toBe(false);
    });
  });

  describe('sanitizeInput', () => {
    test('strips dangerous ASCII control characters', () => {
      const dirty = 'Hello\x00World\x07Test\x1F';
      expect(sanitizeInput(dirty)).toBe('HelloWorldTest');
    });

    test('preserves clean alphanumeric and punctuation characters', () => {
      const clean = 'Software Engineer & Operations Lead (Bangalore)';
      expect(sanitizeInput(clean)).toBe(clean);
    });

    test('handles non-string values safely', () => {
      expect(sanitizeInput(null)).toBe('');
      expect(sanitizeInput(undefined)).toBe('');
      expect(sanitizeInput(12345)).toBe('');
    });
  });
});
