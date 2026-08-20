import { Feather } from '@expo/vector-icons';
import React, { useState } from 'react';
import { Linking, Pressable, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import colors from '@/constants/colors';

const rows = [
  { icon: 'help-circle' as const, title: 'Help centre', detail: 'Answers for setup and shifts' },
  { icon: 'shield' as const, title: 'Privacy & security', detail: 'Your data stays yours' },
  { icon: 'file-text' as const, title: 'Terms of service', detail: 'Last updated Aug 2026' },
];

export default function ProfileScreen() {
  const insets = useSafeAreaInsets();
  const [haptics, setHaptics] = useState(true);
  return (
    <ScrollView style={styles.screen} contentContainerStyle={[styles.content, { paddingTop: Math.max(insets.top, 18), paddingBottom: insets.bottom + 100 }]} showsVerticalScrollIndicator={false}>
      <Text style={styles.eyebrow}>ACCOUNT</Text><Text style={styles.title}>Profile</Text>
      <View style={styles.profileCard}><View style={styles.avatar}><Text style={styles.avatarText}>A</Text></View><View style={styles.profileCopy}><Text style={styles.name}>Arun Alampally</Text><Text style={styles.email}>arun.driver@example.com</Text><View style={styles.proPill}><View style={styles.proDot} /><Text style={styles.proText}>PRO · 23 days left</Text></View></View><Pressable testID="edit-profile" onPress={() => Linking.openURL('mailto:support@autopilot.app')}><Feather name="edit-3" size={18} color={colors.light.mutedForeground} /></Pressable></View>
      <Text style={styles.sectionTitle}>Preferences</Text>
      <View style={styles.card}><View style={styles.preferenceRow}><View style={styles.rowIcon}><Feather name="volume-2" size={17} color={colors.light.primary} /></View><View style={styles.rowCopy}><Text style={styles.rowTitle}>Haptic feedback</Text><Text style={styles.rowDetail}>Feel a subtle tap when Autopilot changes state</Text></View><Switch testID="haptic-toggle" value={haptics} onValueChange={setHaptics} trackColor={{ false: colors.light.muted, true: colors.light.primary }} thumbColor={colors.light.foreground} /></View></View>
      <Text style={styles.sectionTitle}>Support & legal</Text>
      <View style={styles.card}>{rows.map((row) => <Pressable key={row.title} testID={`row-${row.title}`} onPress={() => Linking.openURL(row.title === 'Help centre' ? 'mailto:support@autopilot.app' : 'https://autopilot.app')} style={({ pressed }) => [styles.preferenceRow, pressed && styles.pressed]}><View style={styles.rowIcon}><Feather name={row.icon} size={17} color={colors.light.primary} /></View><View style={styles.rowCopy}><Text style={styles.rowTitle}>{row.title}</Text><Text style={styles.rowDetail}>{row.detail}</Text></View><Feather name="chevron-right" size={18} color={colors.light.mutedForeground} /></Pressable>)}</View>
      <Pressable testID="logout" onPress={() => Linking.openURL('mailto:support@autopilot.app?subject=Account%20help')} style={({ pressed }) => [styles.logout, pressed && styles.pressed]}><Feather name="log-out" size={17} color={colors.light.destructive} /><Text style={styles.logoutText}>Sign out</Text></Pressable>
      <Text style={styles.version}>AUTOPILOT · VERSION 4.0.0</Text>
    </ScrollView>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.light.background }, content: { paddingHorizontal: 20 }, eyebrow: { color: colors.light.primary, fontSize: 11, letterSpacing: 1.5, fontFamily: 'Inter_700Bold' }, title: { color: colors.light.foreground, fontSize: 30, fontFamily: 'Inter_700Bold', marginTop: 7 }, sectionTitle: { color: colors.light.foreground, fontSize: 17, fontFamily: 'Inter_700Bold', marginTop: 27, marginBottom: 11 },
  profileCard: { backgroundColor: colors.light.card, borderWidth: 1, borderColor: colors.light.border, borderRadius: 22, padding: 16, flexDirection: 'row', alignItems: 'center', marginTop: 24 }, avatar: { width: 58, height: 58, borderRadius: 20, backgroundColor: colors.light.primary, alignItems: 'center', justifyContent: 'center' }, avatarText: { color: colors.light.primaryForeground, fontSize: 24, fontFamily: 'Inter_700Bold' }, profileCopy: { flex: 1, marginLeft: 13 }, name: { color: colors.light.foreground, fontSize: 16, fontFamily: 'Inter_700Bold' }, email: { color: colors.light.mutedForeground, fontSize: 12, fontFamily: 'Inter_400Regular', marginTop: 4 }, proPill: { alignSelf: 'flex-start', flexDirection: 'row', alignItems: 'center', backgroundColor: '#A7F3D01C', borderRadius: 10, paddingHorizontal: 8, paddingVertical: 5, marginTop: 8 }, proDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.light.primary, marginRight: 5 }, proText: { color: colors.light.primary, fontSize: 9, letterSpacing: 0.6, fontFamily: 'Inter_700Bold' },
  card: { backgroundColor: colors.light.card, borderWidth: 1, borderColor: colors.light.border, borderRadius: 22, paddingHorizontal: 15 }, preferenceRow: { minHeight: 68, flexDirection: 'row', alignItems: 'center' }, rowIcon: { width: 36, height: 36, borderRadius: 12, backgroundColor: '#A7F3D01C', alignItems: 'center', justifyContent: 'center' }, rowCopy: { flex: 1, marginLeft: 12 }, rowTitle: { color: colors.light.foreground, fontSize: 13, fontFamily: 'Inter_700Bold' }, rowDetail: { color: colors.light.mutedForeground, fontSize: 11, fontFamily: 'Inter_400Regular', marginTop: 3 }, logout: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', gap: 8, marginTop: 30, padding: 14 }, logoutText: { color: colors.light.destructive, fontSize: 13, fontFamily: 'Inter_700Bold' }, version: { color: colors.light.mutedForeground, fontSize: 9, letterSpacing: 1.2, textAlign: 'center', fontFamily: 'Inter_600SemiBold', marginTop: 18 }, pressed: { opacity: 0.72, transform: [{ scale: 0.98 }] },
});