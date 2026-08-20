/**
 * Semantic design tokens for the mobile app.
 *
 * These tokens mirror the naming conventions used in web artifacts (index.css)
 * so that multi-artifact projects share a cohesive visual identity.
 *
 * Replace the placeholder values below with values that match the project's
 * brand. If a sibling web artifact exists, read its index.css and convert the
 * HSL values to hex so both artifacts use the same palette.
 *
 * To add dark mode, add a `dark` key with the same token names.
 * The useColors() hook will automatically pick it up.
 */

const colors = {
  light: {
    text: '#F2F7F3',
    tint: '#A9EBCB',
    background: '#081417',
    foreground: '#F2F7F3',
    card: '#102328',
    cardForeground: '#F2F7F3',
    primary: '#A9EBCB',
    primaryForeground: '#081417',
    secondary: '#17343A',
    secondaryForeground: '#D8E9E1',
    muted: '#1A3035',
    mutedForeground: '#91AAA5',
    accent: '#E9B483',
    accentForeground: '#2A1A10',
    destructive: '#F1847C',
    destructiveForeground: '#2A1110',
    border: '#24444A',
    input: '#294B50',
  },
  dark: {
    text: '#F2F7F3',
    tint: '#A9EBCB',
    background: '#081417',
    foreground: '#F2F7F3',
    card: '#102328',
    cardForeground: '#F2F7F3',
    primary: '#A9EBCB',
    primaryForeground: '#081417',
    secondary: '#17343A',
    secondaryForeground: '#D8E9E1',
    muted: '#1A3035',
    mutedForeground: '#91AAA5',
    accent: '#E9B483',
    accentForeground: '#2A1A10',
    destructive: '#F1847C',
    destructiveForeground: '#2A1110',
    border: '#24444A',
    input: '#294B50',
  },
  radius: 22,
};

export default colors;
