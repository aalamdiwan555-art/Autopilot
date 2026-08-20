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
    text: '#F4F8F6',
    tint: '#A7F3D0',
    background: '#0A171A',
    foreground: '#F4F8F6',
    card: '#12262A',
    cardForeground: '#F4F8F6',
    primary: '#A7F3D0',
    primaryForeground: '#0A171A',
    secondary: '#193338',
    secondaryForeground: '#DCEAE6',
    muted: '#1A3034',
    mutedForeground: '#8CA6A2',
    accent: '#F5B97D',
    accentForeground: '#2B1B0F',
    destructive: '#FF7C73',
    destructiveForeground: '#27100F',
    border: '#244348',
    input: '#244348',
  },
  radius: 22,
};

export default colors;
