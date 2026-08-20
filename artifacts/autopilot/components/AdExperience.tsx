import AsyncStorage from '@react-native-async-storage/async-storage';
import { Feather } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import React, { useEffect, useRef, useState } from 'react';
import { Animated, Modal, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { useColors } from '@/hooks/useColors';

type AdExperienceProps = {
  routeName: string;
};

const STORAGE_KEY = 'autopilot-ad-preferences';

export function AdExperience({ routeName }: AdExperienceProps) {
  const colors = useColors();
  const styles = makeStyles(colors);
  const slide = useRef(new Animated.Value(0)).current;
  const modalProgress = useRef(new Animated.Value(0)).current;
  const shimmer = useRef(new Animated.Value(0)).current;
  const [showTakeover, setShowTakeover] = useState(false);
  const [showRewarded, setShowRewarded] = useState(false);
  const [highFrequency, setHighFrequency] = useState(true);
  const [bannerVisible, setBannerVisible] = useState(true);
  const [seconds, setSeconds] = useState(9);

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY).then((raw) => {
      if (!raw) return;
      try {
        const preferences = JSON.parse(raw) as { highFrequency?: boolean; bannerVisible?: boolean };
        setHighFrequency(preferences.highFrequency !== false);
        setBannerVisible(preferences.bannerVisible !== false);
      } catch {
        // Keep defaults when older local data is malformed.
      }
    }).catch(() => undefined);
  }, []);

  useEffect(() => {
    const first = setTimeout(() => setShowTakeover(true), highFrequency ? 8500 : 18000);
    const recurring = setInterval(() => setShowTakeover(true), highFrequency ? 60000 : 150000);
    return () => {
      clearTimeout(first);
      clearInterval(recurring);
    };
  }, [highFrequency]);

  useEffect(() => {
    if (!showTakeover) return;
    setSeconds(9);
    modalProgress.setValue(0);
    const countdown = setInterval(() => setSeconds((value) => Math.max(0, value - 1)), 1000);
    Animated.spring(modalProgress, { toValue: 1, damping: 16, stiffness: 140, useNativeDriver: true }).start();
    return () => clearInterval(countdown);
  }, [modalProgress, showTakeover]);

  useEffect(() => {
    Animated.spring(slide, { toValue: bannerVisible ? 1 : 0, damping: 18, stiffness: 160, useNativeDriver: true }).start();
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(shimmer, { toValue: 1, duration: 1500, useNativeDriver: true }),
        Animated.timing(shimmer, { toValue: 0, duration: 1500, useNativeDriver: true }),
      ]),
    );
    loop.start();
    return () => loop.stop();
  }, [bannerVisible, shimmer, slide]);

  const persist = (next: { highFrequency?: boolean; bannerVisible?: boolean }) => {
    void AsyncStorage.setItem(STORAGE_KEY, JSON.stringify({
      highFrequency,
      bannerVisible,
      ...next,
    }));
  };

  const closeTakeover = () => {
    setShowTakeover(false);
    persist({});
  };

  const bannerTranslate = slide.interpolate({ inputRange: [0, 1], outputRange: [130, 0] });
  const modalScale = modalProgress.interpolate({ inputRange: [0, 1], outputRange: [0.88, 1] });
  const shimmerTranslate = shimmer.interpolate({ inputRange: [0, 1], outputRange: [-60, 180] });

  return (
    <>
      <Animated.View pointerEvents={bannerVisible ? 'auto' : 'none'} style={[styles.banner, { transform: [{ translateY: bannerTranslate }] }]}>
        <LinearGradient colors={[colors.accent, '#F6D39D']} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={styles.bannerGradient}>
          <Animated.View style={[styles.shimmer, { transform: [{ translateX: shimmerTranslate }] }]} />
          <View style={styles.sponsorBadge}><Feather name="zap" size={11} color={colors.accentForeground} /><Text style={styles.sponsorText}>SPONSORED</Text></View>
          <View style={styles.bannerCopy}>
            <Text style={styles.bannerTitle}>Drive smarter. Save more.</Text>
            <Text style={styles.bannerSubtitle}>{routeName === 'rewards' ? 'Unlock 30 extra reward days today' : 'A driver-first offer is waiting for you'}</Text>
          </View>
          <Pressable accessibilityRole="button" accessibilityLabel="Open sponsored offer" onPress={() => setShowRewarded(true)} style={styles.bannerAction}>
            <Text style={styles.bannerActionText}>OPEN</Text>
            <Feather name="arrow-up-right" size={14} color={colors.accentForeground} />
          </Pressable>
          <Pressable accessibilityRole="button" accessibilityLabel="Dismiss sponsored banner" onPress={() => { setBannerVisible(false); persist({ bannerVisible: false }); }} style={styles.bannerClose}>
            <Feather name="x" size={15} color={colors.accentForeground} />
          </Pressable>
        </LinearGradient>
      </Animated.View>

      <Modal visible={showTakeover} transparent animationType="none" onRequestClose={closeTakeover}>
        <View style={styles.modalBackdrop}>
          <Animated.View style={[styles.takeover, { opacity: modalProgress, transform: [{ scale: modalScale }] }]}>
            <LinearGradient colors={[colors.secondary, '#0D2024']} style={styles.takeoverGradient}>
              <View style={styles.modalTopline}>
                <View style={styles.sponsorBadgeDark}><Feather name="radio" size={11} color={colors.primary} /><Text style={styles.sponsorTextDark}>SPONSORED MOMENT</Text></View>
                <Pressable accessibilityRole="button" accessibilityLabel="Close sponsored ad" onPress={closeTakeover} style={styles.modalClose}><Feather name="x" size={19} color={colors.secondaryForeground} /></Pressable>
              </View>
              <View style={styles.adOrb}><Feather name="navigation" size={32} color={colors.primaryForeground} /></View>
              <Text style={styles.takeoverEyebrow}>YOUR NEXT SHIFT, UPGRADED</Text>
              <Text style={styles.takeoverTitle}>Keep every mile moving.</Text>
              <Text style={styles.takeoverBody}>Watch this short sponsored offer to unlock a boosted Autopilot reward. You choose when to continue.</Text>
              <View style={styles.countdownRow}><View style={styles.countdownDot} /><Text style={styles.countdownText}>OFFER EXPIRES IN {seconds}s</Text></View>
              <Pressable accessibilityRole="button" accessibilityLabel="Watch sponsored reward" onPress={() => { setShowTakeover(false); setShowRewarded(true); }} style={styles.takeoverAction}>
                <Feather name="play" size={16} color={colors.primaryForeground} /><Text style={styles.takeoverActionText}>Watch & unlock reward</Text>
              </Pressable>
              <Pressable accessibilityRole="button" accessibilityLabel="Skip sponsored offer" onPress={closeTakeover} style={styles.skipButton}><Text style={styles.skipText}>Not now</Text></Pressable>
            </LinearGradient>
          </Animated.View>
        </View>
      </Modal>

      <Modal visible={showRewarded} transparent animationType="fade" onRequestClose={() => setShowRewarded(false)}>
        <View style={styles.modalBackdrop}>
          <View style={styles.rewardedCard}>
            <View style={styles.rewardedIcon}><Feather name="check" size={24} color={colors.primaryForeground} /></View>
            <Text style={styles.rewardedEyebrow}>REWARDED SPONSOR</Text>
            <Text style={styles.rewardedTitle}>Reward ready to claim</Text>
            <Text style={styles.rewardedBody}>The ad inventory is ready. In production, this slot connects to the configured rewarded network and confirms the reward server-side.</Text>
            <Pressable accessibilityRole="button" accessibilityLabel="Claim sponsored reward" onPress={() => setShowRewarded(false)} style={styles.takeoverAction}><Text style={styles.takeoverActionText}>Claim reward</Text><Feather name="arrow-right" size={16} color={colors.primaryForeground} /></Pressable>
            <Pressable accessibilityRole="button" accessibilityLabel="Close reward" onPress={() => setShowRewarded(false)} style={styles.skipButton}><Text style={styles.skipText}>Close</Text></Pressable>
          </View>
        </View>
      </Modal>
    </>
  );
}

export function AdControls({ onChange }: { onChange?: (enabled: boolean) => void }) {
  const colors = useColors();
  const styles = makeStyles(colors);
  const [enabled, setEnabled] = useState(true);

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY).then((raw) => {
      if (!raw) return;
      try { setEnabled((JSON.parse(raw) as { highFrequency?: boolean }).highFrequency !== false); } catch { /* defaults */ }
    }).catch(() => undefined);
  }, []);

  const toggle = (value: boolean) => {
    setEnabled(value);
    void AsyncStorage.setItem(STORAGE_KEY, JSON.stringify({ highFrequency: value, bannerVisible: true }));
    onChange?.(value);
  };

  return (
    <View style={styles.adControls}>
      <View style={styles.controlIcon}><Feather name="radio" size={16} color={colors.accent} /></View>
      <View style={styles.controlCopy}><Text style={styles.controlTitle}>High-frequency offers</Text><Text style={styles.controlDetail}>Show more sponsored rewards and limited-time drops</Text></View>
      <Pressable accessibilityRole="switch" accessibilityState={{ checked: enabled }} accessibilityLabel="High-frequency offers" onPress={() => toggle(!enabled)} style={[styles.switch, enabled && styles.switchActive]}><View style={[styles.switchThumb, enabled && styles.switchThumbActive]} /></Pressable>
    </View>
  );
}

function makeStyles(colors: ReturnType<typeof useColors>) {
  return StyleSheet.create({
    banner: { position: 'absolute', left: 12, right: 12, bottom: Platform.OS === 'web' ? 91 : 79, zIndex: 20, borderRadius: 18, overflow: 'hidden', shadowColor: '#000', shadowOpacity: 0.25, shadowRadius: 14, elevation: 12 },
    bannerGradient: { minHeight: 70, paddingHorizontal: 13, paddingVertical: 10, flexDirection: 'row', alignItems: 'center', overflow: 'hidden' },
    shimmer: { position: 'absolute', width: 48, top: -20, bottom: -20, backgroundColor: '#FFFFFF44', transform: [{ rotate: '18deg' }] },
    sponsorBadge: { position: 'absolute', top: 7, left: 12, flexDirection: 'row', alignItems: 'center', gap: 4 },
    sponsorText: { color: colors.accentForeground, fontSize: 7, letterSpacing: 0.75, fontFamily: 'Inter_700Bold' },
    bannerCopy: { flex: 1, paddingTop: 10, paddingRight: 8 },
    bannerTitle: { color: colors.accentForeground, fontSize: 13, fontFamily: 'Inter_700Bold' },
    bannerSubtitle: { color: `${colors.accentForeground}CC`, fontSize: 10, fontFamily: 'Inter_500Medium', marginTop: 3 },
    bannerAction: { backgroundColor: `${colors.accentForeground}18`, borderWidth: 1, borderColor: `${colors.accentForeground}40`, paddingHorizontal: 10, paddingVertical: 9, borderRadius: 11, flexDirection: 'row', alignItems: 'center', gap: 4 },
    bannerActionText: { color: colors.accentForeground, fontSize: 9, fontFamily: 'Inter_700Bold' },
    bannerClose: { position: 'absolute', right: 7, top: 6, padding: 4 },
    modalBackdrop: { flex: 1, backgroundColor: '#020809CC', alignItems: 'center', justifyContent: 'center', padding: 20 },
    takeover: { width: '100%', maxWidth: 390, borderRadius: 28, overflow: 'hidden', shadowColor: '#000', shadowOpacity: 0.4, shadowRadius: 24, elevation: 18 },
    takeoverGradient: { padding: 22, minHeight: 475 },
    modalTopline: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
    sponsorBadgeDark: { flexDirection: 'row', alignItems: 'center', gap: 5 },
    sponsorTextDark: { color: colors.primary, fontSize: 8, letterSpacing: 0.9, fontFamily: 'Inter_700Bold' },
    modalClose: { width: 38, height: 38, borderRadius: 12, backgroundColor: `${colors.foreground}0B`, alignItems: 'center', justifyContent: 'center' },
    adOrb: { width: 78, height: 78, borderRadius: 28, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center', marginTop: 43, shadowColor: colors.primary, shadowOpacity: 0.38, shadowRadius: 20, elevation: 10 },
    takeoverEyebrow: { color: colors.primary, fontSize: 9, letterSpacing: 1.2, fontFamily: 'Inter_700Bold', marginTop: 29 },
    takeoverTitle: { color: colors.foreground, fontSize: 29, lineHeight: 35, fontFamily: 'Inter_700Bold', marginTop: 8 },
    takeoverBody: { color: colors.secondaryForeground, fontSize: 13, lineHeight: 20, fontFamily: 'Inter_400Regular', marginTop: 11 },
    countdownRow: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 23 },
    countdownDot: { width: 7, height: 7, borderRadius: 4, backgroundColor: colors.accent },
    countdownText: { color: colors.accent, fontSize: 9, letterSpacing: 0.8, fontFamily: 'Inter_700Bold' },
    takeoverAction: { height: 53, borderRadius: 16, backgroundColor: colors.primary, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, marginTop: 22 },
    takeoverActionText: { color: colors.primaryForeground, fontSize: 13, fontFamily: 'Inter_700Bold' },
    skipButton: { alignItems: 'center', justifyContent: 'center', minHeight: 44, marginTop: 4 },
    skipText: { color: colors.mutedForeground, fontSize: 12, fontFamily: 'Inter_600SemiBold' },
    rewardedCard: { width: '100%', maxWidth: 370, backgroundColor: colors.card, borderRadius: 26, padding: 23, borderWidth: 1, borderColor: colors.border },
    rewardedIcon: { width: 52, height: 52, borderRadius: 18, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
    rewardedEyebrow: { color: colors.accent, fontSize: 9, letterSpacing: 1.1, fontFamily: 'Inter_700Bold', marginTop: 24 },
    rewardedTitle: { color: colors.foreground, fontSize: 25, fontFamily: 'Inter_700Bold', marginTop: 7 },
    rewardedBody: { color: colors.mutedForeground, fontSize: 12, lineHeight: 19, fontFamily: 'Inter_400Regular', marginTop: 10 },
    adControls: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.border, borderRadius: 20, padding: 14, flexDirection: 'row', alignItems: 'center' },
    controlIcon: { width: 38, height: 38, borderRadius: 13, backgroundColor: `${colors.accent}18`, alignItems: 'center', justifyContent: 'center' },
    controlCopy: { flex: 1, marginLeft: 11 },
    controlTitle: { color: colors.foreground, fontSize: 12, fontFamily: 'Inter_700Bold' },
    controlDetail: { color: colors.mutedForeground, fontSize: 10, lineHeight: 15, fontFamily: 'Inter_400Regular', marginTop: 3 },
    switch: { width: 43, height: 26, borderRadius: 14, backgroundColor: colors.muted, padding: 3, justifyContent: 'center' },
    switchActive: { backgroundColor: colors.accent },
    switchThumb: { width: 20, height: 20, borderRadius: 10, backgroundColor: colors.mutedForeground },
    switchThumbActive: { alignSelf: 'flex-end', backgroundColor: colors.accentForeground },
  });
}