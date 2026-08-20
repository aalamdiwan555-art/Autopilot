import AsyncStorage from '@react-native-async-storage/async-storage';
import { Feather } from '@expo/vector-icons';
import React, { useEffect, useRef, useState } from 'react';
import { Animated, Linking, Platform, Pressable, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useColors } from '@/hooks/useColors';
import { AdControls } from '@/components/AdExperience';

const rows = [
  { icon: 'help-circle' as const, title: 'Help centre', detail: 'Answers for setup and shifts', url: 'mailto:support@autopilot.app' },
  { icon: 'shield' as const, title: 'Privacy & security', detail: 'Your data stays yours', url: 'https://autopilot.app' },
  { icon: 'file-text' as const, title: 'Terms of service', detail: 'Last updated Aug 2026', url: 'https://autopilot.app' },
];

export default function ProfileScreen() {
  const insets = useSafeAreaInsets();
  const colors = useColors();
  const styles = makeStyles(colors);
  const reveal = useRef(new Animated.Value(0)).current;
  const [haptics, setHaptics] = useState(true);

  useEffect(() => {
    AsyncStorage.getItem('autopilot-haptics').then((value) => {
      if (value !== null) setHaptics(value === 'true');
    }).catch(() => undefined);
  }, []);

  useEffect(() => {
    Animated.timing(reveal, { toValue: 1, duration: 650, useNativeDriver: true }).start();
  }, [reveal]);

  const revealStyle = {
    opacity: reveal,
    transform: [{ translateY: reveal.interpolate({ inputRange: [0, 1], outputRange: [16, 0] }) }],
  };

  const changeHaptics = (value: boolean) => {
    setHaptics(value);
    void AsyncStorage.setItem('autopilot-haptics', String(value));
  };

  const openLink = async (url: string) => {
    try {
      await Linking.openURL(url);
    } catch {
      // Keep support and legal actions safe when a platform cannot open a URL.
    }
  };

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={[styles.content, { paddingTop: Math.max(insets.top, Platform.OS === 'web' ? 67 : 18), paddingBottom: insets.bottom + 110 }]}
      showsVerticalScrollIndicator={false}
    >
      <Animated.View style={revealStyle}>
      <View style={styles.topline}><View style={styles.pageMark}><Feather name="user" size={15} color={colors.primaryForeground} /></View><Text style={styles.pageMarkLabel}>DRIVER ACCOUNT</Text></View>
      <Text style={styles.eyebrow}>ACCOUNT</Text>
      <Text style={styles.title}>Profile</Text>

      <View style={styles.profileCard}>
        <View style={styles.avatar}><Text style={styles.avatarText}>A</Text><View style={styles.avatarRing} /></View>
        <View style={styles.profileCopy}><Text style={styles.name}>Arun Alampally</Text><Text style={styles.email}>arun.driver@example.com</Text><View style={styles.proPill}><View style={styles.proDot} /><Text style={styles.proText}>PRO · 23 DAYS LEFT</Text></View></View>
        <Pressable testID="edit-profile" accessibilityRole="button" accessibilityLabel="Contact support to edit profile" onPress={() => openLink('mailto:support@autopilot.app')} style={styles.iconButton}><Feather name="edit-3" size={17} color={colors.mutedForeground} /></Pressable>
      </View>

      <Text style={styles.sectionTitle}>Preferences</Text>
      <View style={styles.card}>
        <View style={styles.preferenceRow}>
          <View style={styles.rowIcon}><Feather name="volume-2" size={16} color={colors.primary} /></View>
          <View style={styles.rowCopy}><Text style={styles.rowTitle}>Haptic feedback</Text><Text style={styles.rowDetail}>Feel a subtle tap when Autopilot changes state</Text></View>
          <Switch testID="haptic-toggle" accessibilityLabel="Haptic feedback" value={haptics} onValueChange={changeHaptics} trackColor={{ false: colors.muted, true: colors.primary }} thumbColor={haptics ? colors.primaryForeground : colors.mutedForeground} />
        </View>
      </View>

      <Text style={styles.sectionTitle}>Offers</Text>
      <AdControls />

      <Text style={styles.sectionTitle}>Support & legal</Text>
      <View style={styles.card}>
        {rows.map((row, index) => (
          <Pressable
            key={row.title}
            testID={`row-${row.title}`}
            accessibilityRole="button"
            onPress={() => openLink(row.url)}
            style={({ pressed }) => [styles.preferenceRow, index < rows.length - 1 && styles.rowDivider, pressed && styles.pressed]}
          >
            <View style={styles.rowIcon}><Feather name={row.icon} size={16} color={colors.primary} /></View>
            <View style={styles.rowCopy}><Text style={styles.rowTitle}>{row.title}</Text><Text style={styles.rowDetail}>{row.detail}</Text></View>
            <Feather name="arrow-up-right" size={16} color={colors.mutedForeground} />
          </Pressable>
        ))}
      </View>

      <Pressable testID="logout" accessibilityRole="button" onPress={() => openLink('mailto:support@autopilot.app?subject=Account%20help')} style={({ pressed }) => [styles.logout, pressed && styles.pressed]}>
        <Feather name="log-out" size={16} color={colors.destructive} /><Text style={styles.logoutText}>Sign out</Text>
      </Pressable>
      <Text style={styles.version}>AUTOPILOT · VERSION 4.0.0</Text>
      </Animated.View>
    </ScrollView>
  );
}

function makeStyles(colors: ReturnType<typeof useColors>) {
  return StyleSheet.create({
    screen: { flex: 1, backgroundColor: colors.background },
    content: { paddingHorizontal: 20 },
    topline: { flexDirection: 'row', alignItems: 'center', gap: 9, marginBottom: 27 },
    pageMark: { width: 28, height: 28, borderRadius: 10, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
    pageMarkLabel: { color: colors.foreground, fontSize: 10, letterSpacing: 1.45, fontFamily: 'Inter_700Bold' },
    eyebrow: { color: colors.primary, fontSize: 10, letterSpacing: 1.3, fontFamily: 'Inter_700Bold' },
    title: { color: colors.foreground, fontSize: 30, fontFamily: 'Inter_700Bold', marginTop: 7 },
    profileCard: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.border, borderRadius: 22, padding: 15, flexDirection: 'row', alignItems: 'center', marginTop: 23 },
    avatar: { width: 57, height: 57, borderRadius: 19, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
    avatarRing: { position: 'absolute', top: -4, right: -4, bottom: -4, left: -4, borderRadius: 23, borderWidth: 1, borderColor: `${colors.primary}3D` },
    avatarText: { color: colors.primaryForeground, fontSize: 23, fontFamily: 'Inter_700Bold' },
    profileCopy: { flex: 1, marginLeft: 13 },
    name: { color: colors.foreground, fontSize: 15, fontFamily: 'Inter_700Bold' },
    email: { color: colors.mutedForeground, fontSize: 11, fontFamily: 'Inter_400Regular', marginTop: 4 },
    proPill: { alignSelf: 'flex-start', flexDirection: 'row', alignItems: 'center', backgroundColor: `${colors.primary}16`, borderRadius: 9, paddingHorizontal: 8, paddingVertical: 5, marginTop: 8 },
    proDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: colors.primary, marginRight: 5 },
    proText: { color: colors.primary, fontSize: 8, letterSpacing: 0.6, fontFamily: 'Inter_700Bold' },
    iconButton: { width: 44, height: 44, borderRadius: 11, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.muted },
    sectionTitle: { color: colors.foreground, fontSize: 16, fontFamily: 'Inter_700Bold', marginTop: 27, marginBottom: 11 },
    card: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.border, borderRadius: 21, paddingHorizontal: 14 },
    preferenceRow: { minHeight: 68, flexDirection: 'row', alignItems: 'center' },
    rowDivider: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.border },
    rowIcon: { width: 36, height: 36, borderRadius: 12, backgroundColor: `${colors.primary}15`, alignItems: 'center', justifyContent: 'center' },
    rowCopy: { flex: 1, marginLeft: 12 },
    rowTitle: { color: colors.foreground, fontSize: 12, fontFamily: 'Inter_700Bold' },
    rowDetail: { color: colors.mutedForeground, fontSize: 10, lineHeight: 15, fontFamily: 'Inter_400Regular', marginTop: 3 },
    logout: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', gap: 8, marginTop: 28, padding: 14, minHeight: 48 },
    logoutText: { color: colors.destructive, fontSize: 12, fontFamily: 'Inter_700Bold' },
    version: { color: colors.mutedForeground, fontSize: 9, letterSpacing: 1.1, textAlign: 'center', fontFamily: 'Inter_600SemiBold', marginTop: 17 },
    pressed: { opacity: 0.72, transform: [{ scale: 0.98 }] },
  });
}