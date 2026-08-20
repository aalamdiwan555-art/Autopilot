import AsyncStorage from '@react-native-async-storage/async-storage';
import { Feather } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import * as Haptics from 'expo-haptics';
import { useFocusEffect } from 'expo-router';
import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Animated, Modal, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useColors } from '@/hooks/useColors';

type Driver = {
  name: string;
  icon: keyof typeof Feather.glyphMap;
  tint: string;
  connected: boolean;
};

const drivers: Driver[] = [
  { name: 'Uber', icon: 'navigation', tint: '#A9EBCB', connected: true },
  { name: 'Ola', icon: 'circle', tint: '#E9B483', connected: true },
  { name: 'Rapido', icon: 'zap', tint: '#C2D8D0', connected: false },
];

const setupCopy = [
  {
    title: 'Turn on notifications',
    body: 'Get a quiet alert when Autopilot accepts a ride or needs your attention.',
    icon: 'bell' as const,
  },
  {
    title: 'Allow floating controls',
    body: 'Pause Autopilot instantly from any driver app without leaving your shift.',
    icon: 'layers' as const,
  },
  {
    title: 'Connect your last app',
    body: 'Rapido is the last supported app waiting for access.',
    icon: 'zap' as const,
  },
];

export default function HomeScreen() {
  const insets = useSafeAreaInsets();
  const colors = useColors();
  const styles = makeStyles(colors);
  const pulse = useRef(new Animated.Value(0)).current;
  const entrance = useRef(new Animated.Value(0)).current;
  const progress = useRef(new Animated.Value(0)).current;
  const [isRunning, setIsRunning] = useState(false);
  const [showSetup, setShowSetup] = useState(false);
  const [setupStep, setSetupStep] = useState(0);
  const [notice, setNotice] = useState('');
  const completed = drivers.filter((driver) => driver.connected).length;

  const loadState = useCallback(async () => {
    try {
      const stored = await AsyncStorage.getItem('autopilot-running');
      setIsRunning(stored === 'true');
    } catch {
      setIsRunning(false);
    }
  }, []);

  useFocusEffect(useCallback(() => { void loadState(); }, [loadState]));

  useEffect(() => {
    Animated.parallel([
      Animated.timing(entrance, { toValue: 1, duration: 650, useNativeDriver: true }),
      Animated.timing(progress, { toValue: completed / 3, duration: 900, delay: 220, useNativeDriver: false }),
    ]).start();
    const animation = Animated.loop(
      Animated.sequence([
        Animated.timing(pulse, { toValue: 1, duration: 1800, useNativeDriver: true }),
        Animated.timing(pulse, { toValue: 0, duration: 1800, useNativeDriver: true }),
      ]),
    );
    animation.start();
    return () => animation.stop();
  }, [completed, entrance, progress, pulse]);

  const glowScale = pulse.interpolate({ inputRange: [0, 1], outputRange: [1, 1.18] });
  const entranceStyle = {
    opacity: entrance,
    transform: [{ translateY: entrance.interpolate({ inputRange: [0, 1], outputRange: [18, 0] }) }],
  };
  const progressWidth = progress.interpolate({ inputRange: [0, 1], outputRange: ['0%', '100%'] });
  const statusText = isRunning ? 'Autopilot is watching' : 'Autopilot is paused';
  const lastAction = useMemo(
    () => isRunning ? 'Ready to accept your next ride' : 'Start a shift when you are ready',
    [isRunning],
  );

  const showNotice = (message: string) => {
    setNotice(message);
    setTimeout(() => setNotice(''), 2600);
  };

  const toggle = async () => {
    const next = !isRunning;
    try {
      await AsyncStorage.setItem('autopilot-running', String(next));
    } catch {
      // Keep the control responsive even when local storage is unavailable.
    }
    setIsRunning(next);
    showNotice(next ? 'Autopilot is live for this shift.' : 'Autopilot paused safely.');
    await Haptics.impactAsync(next ? Haptics.ImpactFeedbackStyle.Medium : Haptics.ImpactFeedbackStyle.Light);
  };

  const completeSetup = () => {
    if (setupStep >= setupCopy.length - 1) {
      setShowSetup(false);
      setSetupStep(0);
      showNotice('Setup complete. You are ready to drive.');
      return;
    }
    setSetupStep((value) => value + 1);
  };

  return (
    <View style={styles.screen}>
      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingTop: Math.max(insets.top, Platform.OS === 'web' ? 67 : 16), paddingBottom: insets.bottom + 112 },
        ]}
        showsVerticalScrollIndicator={false}
      >
        <Animated.View style={[styles.header, entranceStyle]}>
          <View style={styles.brandLockup}>
            <View style={styles.brandMark}><Feather name="crosshair" size={15} color={colors.primaryForeground} /></View>
            <Text style={styles.brandName}>AUTOPILOT</Text>
          </View>
          <Pressable
            testID="profile-avatar"
            accessibilityRole="button"
            accessibilityLabel="Open setup"
            style={({ pressed }) => [styles.avatar, pressed && styles.pressed]}
            onPress={() => setShowSetup(true)}
          >
            <Text style={styles.avatarText}>A</Text>
            <View style={styles.avatarDot} />
          </Pressable>
        </Animated.View>

        <Animated.View style={[styles.greetingRow, entranceStyle]}>
          <View>
            <Text style={styles.eyebrow}>THURSDAY · AUG 20</Text>
            <Text style={styles.title}>Good morning, Arun</Text>
          </View>
          <View style={styles.shiftChip}><View style={styles.shiftChipDot} /><Text style={styles.shiftChipText}>SHIFT 01</Text></View>
        </Animated.View>

        <Animated.View style={entranceStyle}>
        <LinearGradient
          colors={[colors.secondary, '#10262A']}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={styles.hero}
        >
          <View style={styles.heroGridLine} />
          <View style={styles.heroCopy}>
            <View style={styles.livePill}>
              <View style={[styles.liveDot, !isRunning && styles.liveDotPaused]} />
              <Text style={styles.liveText}>{isRunning ? 'LIVE SHIFT' : 'READY TO DRIVE'}</Text>
            </View>
            <Text style={styles.heroTitle}>{statusText}</Text>
            <Text style={styles.heroSubtitle}>{lastAction}</Text>
            <View style={styles.heroFooter}><Feather name="shield" size={14} color={colors.primary} /><Text style={styles.heroFooterText}>You stay in control</Text></View>
          </View>
          <View style={styles.orbit}>
            <Animated.View style={[styles.orbitGlow, { transform: [{ scale: glowScale }], opacity: isRunning ? 0.32 : 0.1 }]} />
            <View style={styles.orbitRing} />
            <Pressable
              testID="autopilot-toggle"
              accessibilityRole="button"
              accessibilityLabel={isRunning ? 'Pause Autopilot' : 'Start Autopilot'}
              onPress={toggle}
              style={({ pressed }) => [styles.startButton, pressed && styles.pressed]}
            >
              <Feather name={isRunning ? 'pause' : 'play'} size={21} color={colors.primaryForeground} />
              <Text style={styles.startButtonText}>{isRunning ? 'Pause' : 'Start'}</Text>
            </Pressable>
          </View>
        </LinearGradient>
        </Animated.View>

        {notice ? (
          <View style={styles.notice} accessibilityLiveRegion="polite">
            <View style={styles.noticeIcon}><Feather name="check" size={14} color={colors.primaryForeground} /></View>
            <Text style={styles.noticeText}>{notice}</Text>
          </View>
        ) : null}

        <View style={styles.sectionHeading}>
          <Text style={styles.sectionTitle}>Your shift</Text>
          <Text style={styles.sectionMeta}>TODAY</Text>
        </View>
        <Animated.View style={[styles.statsRow, entranceStyle]}>
          <View style={[styles.statCard, styles.statCardPrimary]}>
            <View style={styles.statTop}><Feather name="check-circle" size={15} color={colors.primary} /><Text style={styles.statLabel}>RIDES ACCEPTED</Text></View>
            <Text style={styles.statValue}>18</Text>
            <Text style={styles.statHint}>+12% vs last shift</Text>
          </View>
          <View style={styles.statCard}>
            <View style={styles.statTop}><Feather name="trending-up" size={15} color={colors.accent} /><Text style={styles.statLabel}>EST. EARNINGS</Text></View>
            <Text style={styles.statValue}>₹1,240</Text>
            <Text style={styles.statHint}>{'₹68 per ride'}</Text>
          </View>
        </Animated.View>

        <View style={styles.sectionHeading}>
          <Text style={styles.sectionTitle}>Automation setup</Text>
          <Text style={styles.sectionMeta}>{completed}/3 READY</Text>
        </View>
        <Animated.View style={[styles.setupCard, entranceStyle]}>
          <View style={styles.progressMeta}><Text style={styles.progressLabel}>CONNECTION CHECK</Text><Text style={styles.progressPercent}>{Math.round((completed / 3) * 100)}%</Text></View>
          <View style={styles.progressTrack}><Animated.View style={[styles.progressFill, { width: progressWidth }]} /></View>
          <Text style={styles.setupTitle}>{completed === 3 ? 'Everything is connected' : 'Finish your setup'}</Text>
          <Text style={styles.setupBody}>{completed === 3 ? 'Autopilot can now work across your selected driver apps.' : 'A one-minute check keeps your shift smooth and interruption-free.'}</Text>
          <Pressable
            testID="setup-button"
            accessibilityRole="button"
            onPress={() => setShowSetup(true)}
            style={({ pressed }) => [styles.secondaryButton, pressed && styles.pressed]}
          >
            <Text style={styles.secondaryButtonText}>{completed === 3 ? 'Review setup' : 'Complete setup'}</Text>
            <Feather name="arrow-up-right" size={16} color={colors.primary} />
          </Pressable>
        </Animated.View>

        <View style={styles.sectionHeading}>
          <Text style={styles.sectionTitle}>Driver apps</Text>
          <Text style={styles.sectionMeta}>SUPPORTED</Text>
        </View>
        <Animated.View style={[styles.driverList, entranceStyle]}>
          {drivers.map((driver, index) => (
            <View key={driver.name} style={[styles.driverRow, index === drivers.length - 1 && styles.driverRowLast]}>
              <View style={[styles.driverIcon, { backgroundColor: `${driver.tint}18` }]}><Feather name={driver.icon} size={18} color={driver.tint} /></View>
              <View style={styles.driverCopy}><Text style={styles.driverName}>{driver.name}</Text><Text style={styles.driverStatus}>{driver.connected ? 'Connected and ready' : 'Tap setup to connect'}</Text></View>
              <View style={[styles.connectedBadge, !driver.connected && styles.pendingBadge]}><View style={[styles.badgeDot, !driver.connected && styles.badgeDotPending]} /><Text style={[styles.connectedText, !driver.connected && styles.pendingText]}>{driver.connected ? 'Ready' : 'Setup'}</Text></View>
            </View>
          ))}
        </Animated.View>
      </ScrollView>

      <Modal visible={showSetup} transparent animationType="slide" onRequestClose={() => setShowSetup(false)}>
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, { paddingBottom: insets.bottom + 24 }]}>
            <View style={styles.modalHandle} />
            <View style={styles.modalHeader}>
              <View><Text style={styles.modalEyebrow}>SETUP · 0{setupStep + 1}/03</Text><Text style={styles.modalTitle}>{setupCopy[setupStep].title}</Text></View>
              <Pressable testID="setup-close" accessibilityRole="button" accessibilityLabel="Close setup" onPress={() => setShowSetup(false)} style={styles.closeButton}><Feather name="x" size={19} color={colors.foreground} /></Pressable>
            </View>
            <View style={styles.stepDots}>{setupCopy.map((step, index) => <View key={step.title} style={[styles.stepDot, index <= setupStep && styles.stepDotActive]} />)}</View>
            <Text style={styles.modalBody}>{setupCopy[setupStep].body}</Text>
            <View style={styles.stepVisual}>
              <View style={styles.stepIcon}><Feather name={setupCopy[setupStep].icon} size={24} color={colors.primary} /></View>
              <View style={styles.stepVisualCopy}><Text style={styles.stepLabel}>Recommended for smooth shifts</Text><Text style={styles.stepHint}>You can change this anytime in settings.</Text></View>
            </View>
            <Pressable testID="setup-next" accessibilityRole="button" onPress={completeSetup} style={({ pressed }) => [styles.modalButton, pressed && styles.pressed]}>
              <Text style={styles.modalButtonText}>{setupStep >= 2 ? 'Finish setup' : 'Continue'}</Text><Feather name="arrow-right" size={17} color={colors.primaryForeground} />
            </Pressable>
          </View>
        </View>
      </Modal>
    </View>
  );
}

function makeStyles(colors: ReturnType<typeof useColors>) {
  return StyleSheet.create({
    screen: { flex: 1, backgroundColor: colors.background },
    content: { paddingHorizontal: 20 },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 25 },
    brandLockup: { flexDirection: 'row', alignItems: 'center', gap: 9 },
    brandMark: { width: 28, height: 28, borderRadius: 10, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
    brandName: { color: colors.foreground, fontSize: 11, letterSpacing: 1.7, fontFamily: 'Inter_700Bold' },
    avatar: { width: 42, height: 42, borderRadius: 15, backgroundColor: colors.muted, borderWidth: 1, borderColor: colors.border, alignItems: 'center', justifyContent: 'center' },
    avatarText: { color: colors.primary, fontSize: 16, fontFamily: 'Inter_700Bold' },
    avatarDot: { width: 9, height: 9, borderRadius: 5, backgroundColor: colors.primary, borderWidth: 2, borderColor: colors.background, position: 'absolute', right: -2, bottom: -2 },
    greetingRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end', marginBottom: 17 },
    eyebrow: { color: colors.mutedForeground, fontSize: 10, letterSpacing: 1.35, fontFamily: 'Inter_600SemiBold' },
    title: { color: colors.foreground, fontSize: 24, fontFamily: 'Inter_700Bold', marginTop: 6 },
    shiftChip: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.muted, paddingHorizontal: 9, paddingVertical: 7, borderRadius: 10, marginBottom: 2 },
    shiftChipDot: { width: 5, height: 5, borderRadius: 3, backgroundColor: colors.accent, marginRight: 6 },
    shiftChipText: { color: colors.mutedForeground, fontSize: 9, letterSpacing: 0.8, fontFamily: 'Inter_700Bold' },
    hero: { minHeight: 232, borderRadius: 27, padding: 21, flexDirection: 'row', justifyContent: 'space-between', overflow: 'hidden', marginBottom: 14 },
    heroGridLine: { position: 'absolute', width: 220, height: 220, borderRadius: 110, borderWidth: 1, borderColor: `${colors.primary}12`, right: -80, top: -75 },
    heroCopy: { flex: 1, justifyContent: 'space-between', paddingVertical: 2, zIndex: 1 },
    livePill: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-start', backgroundColor: `${colors.background}9C`, borderRadius: 20, paddingHorizontal: 10, paddingVertical: 7 },
    liveDot: { width: 7, height: 7, borderRadius: 4, backgroundColor: colors.primary, marginRight: 7 },
    liveDotPaused: { backgroundColor: colors.accent },
    liveText: { color: colors.primary, fontSize: 9, letterSpacing: 1.1, fontFamily: 'Inter_700Bold' },
    heroTitle: { color: colors.foreground, fontSize: 23, lineHeight: 29, fontFamily: 'Inter_700Bold', maxWidth: 180, marginTop: 27 },
    heroSubtitle: { color: colors.secondaryForeground, fontSize: 12, fontFamily: 'Inter_500Medium', marginTop: 7 },
    heroFooter: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 22 },
    heroFooterText: { color: colors.mutedForeground, fontSize: 11, fontFamily: 'Inter_500Medium' },
    orbit: { width: 113, alignItems: 'center', justifyContent: 'center', marginRight: -4 },
    orbitGlow: { position: 'absolute', width: 114, height: 114, borderRadius: 57, backgroundColor: colors.primary },
    orbitRing: { position: 'absolute', width: 126, height: 126, borderRadius: 63, borderWidth: 1, borderColor: `${colors.primary}38` },
    startButton: { width: 86, height: 86, borderRadius: 43, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', gap: 4, shadowColor: colors.primary, shadowOpacity: 0.25, shadowRadius: 18, elevation: 8 },
    startButtonText: { color: colors.primaryForeground, fontSize: 12, fontFamily: 'Inter_700Bold' },
    pressed: { opacity: 0.8, transform: [{ scale: 0.97 }] },
    notice: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.secondary, padding: 12, borderRadius: 15, marginBottom: 14, gap: 9 },
    noticeIcon: { width: 22, height: 22, borderRadius: 8, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
    noticeText: { color: colors.secondaryForeground, fontSize: 12, fontFamily: 'Inter_500Medium' },
    sectionHeading: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 13, marginBottom: 11 },
    sectionTitle: { color: colors.foreground, fontSize: 16, fontFamily: 'Inter_700Bold' },
    sectionMeta: { color: colors.mutedForeground, fontSize: 9, letterSpacing: 1, fontFamily: 'Inter_700Bold' },
    statsRow: { flexDirection: 'row', gap: 10 },
    statCard: { flex: 1, backgroundColor: colors.card, borderRadius: 20, padding: 15, borderWidth: 1, borderColor: colors.border, minHeight: 125 },
    statCardPrimary: { backgroundColor: `${colors.primary}0B` },
    statTop: { flexDirection: 'row', alignItems: 'center', gap: 7 },
    statLabel: { color: colors.mutedForeground, fontSize: 8, letterSpacing: 0.65, fontFamily: 'Inter_700Bold' },
    statValue: { color: colors.foreground, fontSize: 23, fontFamily: 'Inter_700Bold', marginTop: 15 },
    statHint: { color: colors.primary, fontSize: 10, fontFamily: 'Inter_500Medium', marginTop: 6 },
    setupCard: { backgroundColor: colors.card, borderRadius: 21, padding: 16, borderWidth: 1, borderColor: colors.border },
    progressMeta: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 8 },
    progressLabel: { color: colors.mutedForeground, fontSize: 8, letterSpacing: 0.8, fontFamily: 'Inter_700Bold' },
    progressPercent: { color: colors.primary, fontSize: 9, fontFamily: 'Inter_700Bold' },
    progressTrack: { height: 5, backgroundColor: colors.muted, borderRadius: 4, overflow: 'hidden', marginBottom: 16 },
    progressFill: { height: 5, backgroundColor: colors.primary, borderRadius: 4 },
    setupTitle: { color: colors.foreground, fontSize: 15, fontFamily: 'Inter_700Bold' },
    setupBody: { color: colors.mutedForeground, fontSize: 12, lineHeight: 18, fontFamily: 'Inter_400Regular', marginTop: 5, maxWidth: 300 },
    secondaryButton: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-start', gap: 7, paddingTop: 11, minHeight: 44 },
    secondaryButtonText: { color: colors.primary, fontSize: 12, fontFamily: 'Inter_700Bold' },
    driverList: { backgroundColor: colors.card, borderRadius: 21, paddingHorizontal: 15, borderWidth: 1, borderColor: colors.border },
    driverRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 13, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.border },
    driverRowLast: { borderBottomWidth: 0 },
    driverIcon: { width: 38, height: 38, borderRadius: 13, alignItems: 'center', justifyContent: 'center' },
    driverCopy: { flex: 1, marginLeft: 12 },
    driverName: { color: colors.foreground, fontSize: 13, fontFamily: 'Inter_700Bold' },
    driverStatus: { color: colors.mutedForeground, fontSize: 11, fontFamily: 'Inter_400Regular', marginTop: 3 },
    connectedBadge: { backgroundColor: `${colors.primary}15`, borderRadius: 10, paddingHorizontal: 9, paddingVertical: 6, flexDirection: 'row', alignItems: 'center', gap: 5 },
    badgeDot: { width: 5, height: 5, borderRadius: 3, backgroundColor: colors.primary },
    connectedText: { color: colors.primary, fontSize: 9, fontFamily: 'Inter_700Bold' },
    pendingBadge: { backgroundColor: `${colors.accent}18` },
    badgeDotPending: { backgroundColor: colors.accent },
    pendingText: { color: colors.accent },
    modalBackdrop: { flex: 1, backgroundColor: `${colors.background}D9`, justifyContent: 'flex-end' },
    modalCard: { backgroundColor: colors.card, borderTopLeftRadius: 29, borderTopRightRadius: 29, paddingHorizontal: 22, paddingTop: 12, borderTopWidth: 1, borderColor: colors.border },
    modalHandle: { alignSelf: 'center', width: 40, height: 4, borderRadius: 3, backgroundColor: colors.border, marginBottom: 22 },
    modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
    modalEyebrow: { color: colors.primary, fontSize: 9, letterSpacing: 1.3, fontFamily: 'Inter_700Bold' },
    modalTitle: { color: colors.foreground, fontSize: 23, lineHeight: 29, fontFamily: 'Inter_700Bold', marginTop: 8, maxWidth: 280 },
    closeButton: { width: 44, height: 44, borderRadius: 12, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.muted },
    stepDots: { flexDirection: 'row', gap: 5, marginTop: 21 },
    stepDot: { height: 4, flex: 1, borderRadius: 3, backgroundColor: colors.muted },
    stepDotActive: { backgroundColor: colors.primary },
    modalBody: { color: colors.mutedForeground, fontSize: 13, lineHeight: 20, fontFamily: 'Inter_400Regular', marginTop: 17 },
    stepVisual: { backgroundColor: colors.secondary, borderRadius: 17, padding: 14, flexDirection: 'row', alignItems: 'center', marginTop: 21, gap: 12 },
    stepIcon: { width: 47, height: 47, borderRadius: 15, backgroundColor: `${colors.primary}17`, alignItems: 'center', justifyContent: 'center' },
    stepVisualCopy: { flex: 1 },
    stepLabel: { color: colors.secondaryForeground, fontSize: 12, fontFamily: 'Inter_700Bold' },
    stepHint: { color: colors.mutedForeground, fontSize: 10, fontFamily: 'Inter_400Regular', marginTop: 4 },
    modalButton: { height: 54, backgroundColor: colors.primary, borderRadius: 16, flexDirection: 'row', justifyContent: 'center', alignItems: 'center', gap: 8, marginTop: 21 },
    modalButtonText: { color: colors.primaryForeground, fontSize: 13, fontFamily: 'Inter_700Bold' },
  });
}