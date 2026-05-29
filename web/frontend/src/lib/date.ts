/**
 * Date utilities that use local time rather than UTC, preventing the
 * timezone-offset issue where `new Date().toISOString()` can roll the
 * displayed date back by one day for users west of UTC.
 */

/**
 * Format a Date as a local-time ISO date string (YYYY-MM-DD).
 *
 * Using `getFullYear` / `getMonth` / `getDate` reads the date components in
 * the user's local timezone instead of UTC, so the value stays correct across
 * all timezones.
 *
 * @param d - The date to format. Defaults to today.
 *
 * @example
 * toLocalISODate()            // '2026-05-29' (today in local time)
 * toLocalISODate(new Date())  // same
 */
export function toLocalISODate(d: Date = new Date()): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
