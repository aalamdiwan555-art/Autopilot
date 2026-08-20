import AsyncStorage from '@react-native-async-storage/async-storage';
import { Feather } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import * as Haptics from 'expo-haptics';
import { useFocusEffect } from 'expo-router';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Animated, Modal, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import colors from '@/constants/colors';

type Driver = { name: string; icon: keyof typeof Feather.glyphMap; tint: string; connected: boolean };

const drivers: Driver[] = [
  { name: 'Uber', icon: 'navigation', tint: colors.light.primary, connected: true },
  { name: 'Ola', icon: 'circle', tint: colors.light.accent, connected: true },
  { name: 'Rapido', icon: 'zap', tint: '#F9D56E', connected: false },
];

export default function HomeScreen() {
  const insets = useSafeAreaInsets();
  const pulse = useRef(new Animated.Value(0)).current;
  const [isRunning, setIsRunning] = useState(false);
  const [showSetup, setShowSetup] = useState(false);
  const [setupStep, setSetupStep] = useState(0);
  const [notice, setNotice] = useState('');

  const loadState = useCallback(async () => {
    const stored = await AsyncStorage.getItem('autopilot-running');
    setIsRunning(stored === 'true');
  }, []);

  useFocusEffect(useCallback(() => { void loadState(); }, [loadState]));

  useEffect(() => {
    const animation = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, { toValue: 1, duration: 1800, useNativeDriver: true }),
        Animated.timing(pulse, { toValue: 0, duration: 1800, useNativeDriver: true }),
      ]),
    );
    animation.start();
    return () => animation.stop();
  }, [pulse]);

  const glowScale = pulse.interpolate({ inputRange: [0, 1], outputRange: [1, 1.18] });
  const completed = drivers.filter((driver) => driver.connected).length;
  const statusText = isRunning ? 'Autopilot is watching' : 'Autopilot is paused';
  const lastAction = useMemo(() => isRunning ? 'Ready to accept your next ride' : 'Start a shift when you are ready', [isRunning]);

  const toggle = async () => {
    const next = !isRunning;
    await AsyncStorage.setItem('autopilot-running', String(next));
    setIsRunning(next);
    setNotice(next ? 'Autopilot is live for this shift.' : 'Autopilot paused safely.');
    await Haptics.impactAsync(next ? Haptics.ImpactFeedbackStyle.Medium : Haptics.ImpactFeedbackStyle.Light);
    setTimeout(() => setNotice(''), 2600);
  };

  const completeSetup = () => {
    setSetupStep((value) => Math.min(value + 1, 2));
    if (setupStep >= 2) {
      setShowSetup(false);
      setNotice('Setup complete. You are ready to drive.');
      setTimeout(() => setNotice(''), 2600);
    }
  };

  return (
    <View style={styles.screen}>
      <ScrollView
        contentContainerStyle={[styles.content, { paddingTop: Math.max(insets.top, Platform.OS === 'web' ? 67 : 18), paddingBottom: insets.bottom + 110 }]}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.header}>
          <View>
            <Text style={styles.eyebrow}>THURSDAY · AUG 20</Text>
            <Text style={styles.title}>Good morning, Arun</Text>
          </View>
          <Pressable testID="profile-avatar" style={styles.avatar} onPress={() => setShowSetup(true)}>
            <Text style={styles.avatarText}>A</Text>
            <View style={styles.avatarDot} />
          </Pressable>
        </View>

        <LinearGradient colors={[colors.light.secondary, '#1A3E3D']} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={styles.hero}>
          <View style={styles.heroCopy}>
            <View style={styles.livePill}><View style={styles.liveDot} /><Text style={styles.liveText}>{isRunning ? 'LIVE SHIFT' : 'READY TO DRIVE'}</Text></View>
            <Text style={styles.heroTitle}>{statusText}</Text>
            <Text style={styles.heroSubtitle}>{lastAction}</Text>
          </View>
          <View style={styles.orbit}>
            <Animated.View style={[styles.orbitGlow, { transform: [{ scale: glowScale }], opacity: isRunning ? 0.35 : 0.12 }]} />
            <Pressable testID="autopilot-toggle" onPress={toggle} style={({ pressed }) => [styles.startButton, pressed && styles.pressed]}>
              <Feather name={isRunning ? 'pause' : 'play'} size={22} color={colors.light.primaryForeground} />
              <Text style={styles.startButtonText}>{isRunning ? 'Pause' : 'Start'}</Text>
            </Pressable>
          </View>
        </LinearGradient>

        {notice ? <Animated.View style={styles.notice}><Feather name="check-circle" size={17} color={colors.light.primary} /><Text style={styles.noticeText}>{notice}</Text></Animated.View> : null}

        <View style={styles.sectionHeading}><Text style={styles.sectionTitle}>Your shift</Text><Text style={styles.sectionMeta}>Today</Text></View>
        <View style={styles.statsRow}>
          <View style={styles.statCard}><Text style={styles.statLabel}>RIDES ACCEPTED</Text><Text style={styles.statValue}>18</Text><Text style={styles.statHint}>+12% vs last shift</Text></View>
          <View style={styles.statCard}><Text style={styles.statLabel}>EST. EARNINGS</Text><Text style={styles.statValue}>₹1,240</Text><Text style={styles.statHint}>₹68 per ride</Text></View>
        </View>

        <View style={styles.sectionHeading}><Text style={styles.sectionTitle}>Automation setup</Text><Text style={styles.sectionMeta}>{completed}/3 ready</Text></View>
        <View style={styles.setupCard}>
          <View style={styles.progressTrack}><View style={[styles.progressFill, { width: `${Math.round((completed / 3) * 100)}%` }]} /></View>
          <Text style={styles.setupTitle}>{completed === 3 ? 'Everything is connected' : 'Finish your setup'}</Text>
          <Text style={styles.setupBody}>{completed === 3 ? 'Autopilot can now work across your selected driver apps.' : 'A one-minute check keeps your shift smooth and interruption-free.'}</Text>
          <Pressable testID="setup-button" onPress={() => setShowSetup(true)} style={({ pressed }) => [styles.secondaryButton, pressed && styles.pressed]}>
            <Text style={styles.secondaryButtonText}>{completed === 3 ? 'Review setup' : 'Complete setup'}</Text>
            <Feather name="arrow-up-right" size={17} color={colors.light.primary} />
          </Pressable>
        </View>

        <View style={styles.sectionHeading}><Text style={styles.sectionTitle}>Driver apps</Text><Text style={styles.sectionMeta}>Supported</Text></View>
        <View style={styles.driverList}>{drivers.map((driver) => (
          <View key={driver.name} style={styles.driverRow}>
            <View style={[styles.driverIcon, { backgroundColor: `${driver.tint}1C` }]}><Feather name={driver.icon} size={19} color={driver.tint} /></View>
            <View style={styles.driverCopy}><Text style={styles.driverName}>{driver.name}</Text><Text style={styles.driverStatus}>{driver.connected ? 'Connected and ready' : 'Tap to connect'}</Text></View>
            <View style={[styles.connectedBadge, !driver.connected && styles.pendingBadge]}><Text style={[styles.connectedText, !driver.connected && styles.pendingText]}>{driver.connected ? 'Ready' : 'Setup'}</Text></View>
          </View>
        ))}</View>
      </ScrollView>

      <Modal visible={showSetup} transparent animationType="slide" onRequestClose={() => setShowSetup(false)}>
        <View style={styles.modalBackdrop}><View style={[styles.modalCard, { paddingBottom: insets.bottom + 24 }]}>
          <View style={styles.modalHandle} />
          <View style={styles.modalHeader}><View><Text style={styles.modalEyebrow}>SETUP · 0{setupStep + 1}/03</Text><Text style={styles.modalTitle}>{setupStep === 0 ? 'Turn on notifications' : setupStep === 1 ? 'Allow floating controls' : 'Connect your last app'}</Text></View><Pressable onPress={() => setShowSetup(false)} style={styles.closeButton}><Feather name="x" size={20} color={colors.light.foreground} /></Pressable></View>
          <Text style={styles.modalBody}>{setupStep === 0 ? 'Get a quiet alert when Autopilot accepts a ride or needs your attention.' : setupStep === 1 ? 'The floating control lets you pause Autopilot instantly from any driver app.' : 'Rapido is the last supported app waiting for access.'}</Text>
          <View style={styles.stepVisual}><View style={styles.stepIcon}><Feather name={setupStep === 0 ? 'bell' : setupStep === 1 ? 'layers' : 'zap'} size={28} color={colors.light.primary} /></View><View><Text style={styles.stepLabel}>Recommended</Text><Text style={styles.stepHint}>You can change this anytime in settings.</Text></View></View>
          <Pressable testID="setup-next" onPress={completeSetup} style={({ pressed }) => [styles.modalButton, pressed && styles.pressed]}><Text style={styles.modalButtonText}>{setupStep >= 2 ? 'Finish setup' : 'Continue'}</Text><Feather name="arrow-right" size={18} color={colors.light.primaryForeground} /></Pressable>
        </View></View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.light.background },
  content: { paddingHorizontal: 20 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 22 },
  eyebrow: { color: colors.light.mutedForeground, fontSize: 11, letterSpacing: 1.5, fontFamily: 'Inter_600SemiBold' },
  title: { color: colors.light.foreground, fontSize: 25, fontFamily: 'Inter_700Bold', marginTop: 6 },
  avatar: { width: 44, height: 44, borderRadius: 16, backgroundColor: colors.light.primary, alignItems: 'center', justifyContent: 'center' },
  avatarText: { color: colors.light.primaryForeground, fontSize: 17, fontFamily: 'Inter_700Bold' },
  avatarDot: { width: 9, height: 9, borderRadius: 5, backgroundColor: colors.light.accent, borderWidth: 2, borderColor: colors.light.background, position: 'absolute', right: -2, bottom: -2 },
  hero: { minHeight: 208, borderRadius: 28, padding: 22, flexDirection: 'row', justifyContent: 'space-between', overflow: 'hidden', marginBottom: 14 },
  heroCopy: { flex: 1, justifyContent: 'space-between', paddingVertical: 3 },
  livePill: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-start', backgroundColor: '#0A171A88', borderRadius: 20, paddingHorizontal: 10, paddingVertical: 7 },
  liveDot: { width: 7, height: 7, borderRadius: 4, backgroundColor: colors.light.primary, marginRight: 7 },
  liveText: { color: colors.light.primary, fontSize: 10, letterSpacing: 1.2, fontFamily: 'Inter_700Bold' },
  heroTitle: { color: colors.light.foreground, fontSize: 24, lineHeight: 30, fontFamily: 'Inter_700Bold', maxWidth: 180, marginTop: 24 },
  heroSubtitle: { color: '#B5CBC5', fontSize: 13, fontFamily: 'Inter_500Medium', marginTop: 8 },
  orbit: { width: 112, alignItems: 'center', justifyContent: 'center' },
  orbitGlow: { position: 'absolute', width: 112, height: 112, borderRadius: 56, backgroundColor: colors.light.primary },
  startButton: { width: 86, height: 86, borderRadius: 43, backgroundColor: colors.light.primary, alignItems: 'center', justifyContent: 'center', gap: 4, shadowColor: colors.light.primary, shadowOpacity: 0.3, shadowRadius: 18, elevation: 8 },
  startButtonText: { color: colors.light.primaryForeground, fontSize: 12, fontFamily: 'Inter_700Bold' },
  pressed: { opacity: 0.8, transform: [{ scale: 0.97 }] },
  notice: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.light.secondary, padding: 13, borderRadius: 16, marginBottom: 18, gap: 9 },
  noticeText: { color: colors.light.secondaryForeground, fontSize: 13, fontFamily: 'Inter_500Medium' },
  sectionHeading: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 12, marginBottom: 11 },
  sectionTitle: { color: colors.light.foreground, fontSize: 17, fontFamily: 'Inter_700Bold' },
  sectionMeta: { color: colors.light.mutedForeground, fontSize: 12, fontFamily: 'Inter_500Medium' },
  statsRow: { flexDirection: 'row', gap: 10 },
  statCard: { flex: 1, backgroundColor: colors.light.card, borderRadius: 20, padding: 16, borderWidth: 1, borderColor: colors.light.border },
  statLabel: { color: colors.light.mutedForeground, fontSize: 9, letterSpacing: 0.8, fontFamily: 'Inter_700Bold' },
  statValue: { color: colors.light.foreground, fontSize: 24, fontFamily: 'Inter_700Bold', marginTop: 12 },
  statHint: { color: colors.light.primary, fontSize: 11, fontFamily: 'Inter_500Medium', marginTop: 7 },
  setupCard: { backgroundColor: colors.light.card, borderRadius: 22, padding: 17, borderWidth: 1, borderColor: colors.light.border },
  progressTrack: { height: 5, backgroundColor: colors.light.muted, borderRadius: 4, overflow: 'hidden', marginBottom: 16 },
  progressFill: { height: 5, backgroundColor: colors.light.primary, borderRadius: 4 },
  setupTitle: { color: colors.light.foreground, fontSize: 16, fontFamily: 'Inter_700Bold' },
  setupBody: { color: colors.light.mutedForeground, fontSize: 13, lineHeight: 19, fontFamily: 'Inter_400Regular', marginTop: 5, maxWidth: 290 },
  secondaryButton: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-start', gap: 7, paddingTop: 15 },
  secondaryButtonText: { color: colors.light.primary, fontSize: 13, fontFamily: 'Inter_700Bold' },
  driverList: { backgroundColor: colors.light.card, borderRadius: 22, paddingHorizontal: 15, borderWidth: 1, borderColor: colors.light.border },
  driverRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 14 },
  driverIcon: { width: 38, height: 38, borderRadius: 13, alignItems: 'center', justifyContent: 'center' },
  driverCopy: { flex: 1, marginLeft: 12 },
  driverName: { color: colors.light.foreground, fontSize: 14, fontFamily: 'Inter_700Bold' },
  driverStatus: { color: colors.light.mutedForeground, fontSize: 11, fontFamily: 'Inter_400Regular', marginTop: 3 },
  connectedBadge: { backgroundColor: '#A7F3D01C', borderRadius: 12, paddingHorizontal: 10, paddingVertical: 6 },
  connectedText: { color: colors.light.primary, fontSize: 10, fontFamily: 'Inter_700Bold' },
  pendingBadge: { backgroundColor: '#F5B97D1C' },
  pendingText: { color: colors.light.accent },
  modalBackdrop: { flex: 1, backgroundColor: '#00000099', justifyContent: 'flex-end' },
  modalCard: { backgroundColor: colors.light.card, borderTopLeftRadius: 30, borderTopRightRadius: 30, paddingHorizontal: 22, paddingTop: 12 },
  modalHandle: { alignSelf: 'center', width: 42, height: 5, borderRadius: 3, backgroundColor: colors.light.border, marginBottom: 22 },
  modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  modalEyebrow: { color: colors.light.primary, fontSize: 10, letterSpacing: 1.3, fontFamily: 'Inter_700Bold' },
  modalTitle: { color: colors.light.foreground, fontSize: 24, lineHeight: 30, fontFamily: 'Inter_700Bold', marginTop: 8, maxWidth: 285 },
  closeButton: { width: 36, height: 36, borderRadius: 13, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.light.muted },
  modalBody: { color: colors.light.mutedForeground, fontSize: 14, lineHeight: 21, fontFamily: 'Inter_400Regular', marginTop: 16 },
  stepVisual: { backgroundColor: colors.light.secondary, borderRadius: 18, padding: 15, flexDirection: 'row', alignItems: 'center', marginTop: 22, gap: 12 },
  stepIcon: { width: 48, height: 48, borderRadius: 16, backgroundColor: '#A7F3D01C', alignItems: 'center', justifyContent: 'center' },
  stepLabel: { color: colors.light.secondaryForeground, fontSize: 13, fontFamily: 'Inter_700Bold' },
  stepHint: { color: colors.light.mutedForeground, fontSize: 11, fontFamily: 'Inter_400Regular', marginTop: 4 },
  modalButton: { height: 54, backgroundColor: colors.light.primary, borderRadius: 17, flexDirection: 'row', justifyContent: 'center', alignItems: 'center', gap: 8, marginTop: 22 },
  modalButtonText: { color: colors.light.primaryForeground, fontSize: 14, fontFamily: 'Inter_700Bold' },
});
