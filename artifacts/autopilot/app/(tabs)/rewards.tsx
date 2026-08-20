import { Feather } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import React, { useEffect, useRef, useState } from 'react';
import { Animated, Platform, Pressable, ScrollView, Share, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useColors } from '@/hooks/useColors';

export default function RewardsScreen() {
  const insets = useSafeAreaInsets();
  const colors = useColors();
  const styles = makeStyles(colors);
  const reveal = useRef(new Animated.Value(0)).current;
  const [copied, setCopied] = useState(false);
  useEffect(() => {
    Animated.timing(reveal, { toValue: 1, duration: 650, useNativeDriver: true }).start();
  }, [reveal]);
  const revealStyle = {
    opacity: reveal,
    transform: [{ translateY: reveal.interpolate({ inputRange: [0, 1], outputRange: [16, 0] }) }],
  };

  const copyCode = async () => {
    if (Platform.OS === 'web' && typeof navigator !== 'undefined' && navigator.clipboard) {
      await navigator.clipboard.writeText('ARUN-7K2P');
    } else {
      await Share.share({ message: 'ARUN-7K2P' });
    }
    setCopied(true);
    await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    setTimeout(() => setCopied(false), 2200);
  };

  const share = async () => {
    await Share.share({ message: 'Join me on Autopilot and get your first shift covered. Use my code ARUN-7K2P.' });
  };

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={[styles.content, { paddingTop: Math.max(insets.top, Platform.OS === 'web' ? 67 : 18), paddingBottom: insets.bottom + 110 }]}
      showsVerticalScrollIndicator={false}
    >
      <Animated.View style={revealStyle}>
      <View style={styles.topline}><View style={styles.pageMark}><Feather name="gift" size={15} color={colors.primaryForeground} /></View><Text style={styles.pageMarkLabel}>DRIVER CIRCLE</Text></View>
      <Text style={styles.eyebrow}>A LITTLE EXTRA FOR THE CREW</Text>
      <Text style={styles.title}>Refer & earn</Text>
      <Text style={styles.subtitle}>Bring a driver friend on board. You both get seven days of Autopilot Pro.</Text>

      <View style={styles.rewardCard}>
        <View style={styles.rewardOrb}><Feather name="gift" size={22} color={colors.primaryForeground} /></View>
        <Text style={styles.rewardKicker}>YOUR CURRENT REWARD</Text>
        <Text style={styles.rewardValue}>7 days</Text>
        <Text style={styles.rewardLabel}>earned for every friend who starts a shift</Text>
        <View style={styles.divider} />
        <View style={styles.rewardBottom}>
          <View><Text style={styles.smallLabel}>TOTAL EARNED</Text><Text style={styles.smallValue}>14 days</Text></View>
          <View><Text style={styles.smallLabel}>FRIENDS JOINED</Text><Text style={styles.smallValue}>2</Text></View>
          <View><Text style={styles.smallLabel}>STATUS</Text><Text style={styles.smallValue}>Active</Text></View>
        </View>
      </View>

      <View style={styles.sectionHeader}><Text style={styles.sectionTitle}>Your invite code</Text><Text style={styles.sectionMeta}>READY TO SHARE</Text></View>
      <View style={styles.codeCard}>
        <View><Text style={styles.codeLabel}>SHARE THIS CODE</Text><Text style={styles.code}>ARUN-7K2P</Text></View>
        <Pressable testID="copy-code" accessibilityRole="button" accessibilityLabel="Copy invite code" onPress={copyCode} style={({ pressed }) => [styles.copyButton, pressed && styles.pressed]}>
          <Feather name={copied ? 'check' : 'copy'} size={17} color={colors.primaryForeground} />
        </Pressable>
      </View>
      <Pressable testID="share-referral" accessibilityRole="button" onPress={share} style={({ pressed }) => [styles.shareButton, pressed && styles.pressed]}>
        <Feather name="send" size={16} color={colors.primaryForeground} /><Text style={styles.shareText}>Share with a friend</Text>
      </Pressable>
      {copied ? <View style={styles.copiedNotice}><Feather name="check-circle" size={15} color={colors.primary} /><Text style={styles.copiedText}>Code copied to your clipboard</Text></View> : null}

      <View style={styles.tip}><View style={styles.tipIcon}><Feather name="info" size={15} color={colors.accent} /></View><Text style={styles.tipText}>Your reward is added automatically after your friend completes their first active shift.</Text></View>

      <View style={styles.sectionHeader}><Text style={styles.sectionTitle}>Recent rewards</Text><Text style={styles.sectionMeta}>2 UNLOCKED</Text></View>
      <View style={styles.activityCard}>
        <ActivityRow initial="S" name="Sanjay joined Autopilot" date="Aug 18 · reward unlocked" styles={styles} colors={colors} />
        <ActivityRow initial="R" name="Rahul joined Autopilot" date="Aug 12 · reward unlocked" styles={styles} colors={colors} />
      </View>
      </Animated.View>
    </ScrollView>
  );
}

function ActivityRow({ initial, name, date, styles, colors }: { initial: string; name: string; date: string; styles: ReturnType<typeof makeStyles>; colors: ReturnType<typeof useColors> }) {
  return (
    <View style={styles.activityRow}>
      <View style={styles.activityAvatar}><Text style={styles.activityInitial}>{initial}</Text></View>
      <View style={styles.activityCopy}><Text style={styles.activityTitle}>{name}</Text><Text style={styles.activityDate}>{date}</Text></View>
      <Text style={styles.activityReward}>+7 days</Text>
      <Feather name="check" size={14} color={colors.primary} style={styles.activityCheck} />
    </View>
  );
}

function makeStyles(colors: ReturnType<typeof useColors>) {
  return StyleSheet.create({
    screen: { flex: 1, backgroundColor: colors.background },
    content: { paddingHorizontal: 20 },
    topline: { flexDirection: 'row', alignItems: 'center', gap: 9, marginBottom: 27 },
    pageMark: { width: 28, height: 28, borderRadius: 10, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
    pageMarkLabel: { color: colors.foreground, fontSize: 10, letterSpacing: 1.55, fontFamily: 'Inter_700Bold' },
    eyebrow: { color: colors.primary, fontSize: 10, letterSpacing: 1.25, fontFamily: 'Inter_700Bold' },
    title: { color: colors.foreground, fontSize: 30, fontFamily: 'Inter_700Bold', marginTop: 7 },
    subtitle: { color: colors.mutedForeground, fontSize: 13, lineHeight: 20, fontFamily: 'Inter_400Regular', marginTop: 8, maxWidth: 330 },
    rewardCard: { backgroundColor: colors.secondary, borderRadius: 25, padding: 19, marginTop: 23, overflow: 'hidden', borderWidth: 1, borderColor: colors.border },
    rewardOrb: { width: 49, height: 49, borderRadius: 17, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
    rewardKicker: { color: colors.mutedForeground, fontSize: 9, letterSpacing: 1, fontFamily: 'Inter_700Bold', marginTop: 22 },
    rewardValue: { color: colors.foreground, fontSize: 38, fontFamily: 'Inter_700Bold', marginTop: 5 },
    rewardLabel: { color: colors.secondaryForeground, fontSize: 12, fontFamily: 'Inter_500Medium', marginTop: 2 },
    divider: { height: 1, backgroundColor: colors.border, marginVertical: 19 },
    rewardBottom: { flexDirection: 'row', justifyContent: 'space-between' },
    smallLabel: { color: colors.mutedForeground, fontSize: 8, letterSpacing: 0.85, fontFamily: 'Inter_700Bold' },
    smallValue: { color: colors.foreground, fontSize: 15, fontFamily: 'Inter_700Bold', marginTop: 6 },
    sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'baseline', marginTop: 27, marginBottom: 11 },
    sectionTitle: { color: colors.foreground, fontSize: 16, fontFamily: 'Inter_700Bold' },
    sectionMeta: { color: colors.mutedForeground, fontSize: 9, letterSpacing: 0.85, fontFamily: 'Inter_700Bold' },
    codeCard: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.border, borderRadius: 19, padding: 15, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
    codeLabel: { color: colors.mutedForeground, fontSize: 8, letterSpacing: 1, fontFamily: 'Inter_700Bold' },
    code: { color: colors.primary, fontSize: 22, letterSpacing: 1.4, fontFamily: 'Inter_700Bold', marginTop: 6 },
    copyButton: { width: 44, height: 44, borderRadius: 14, backgroundColor: colors.primary, alignItems: 'center', justifyContent: 'center' },
    shareButton: { height: 53, borderRadius: 16, backgroundColor: colors.primary, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, marginTop: 10 },
    shareText: { color: colors.primaryForeground, fontSize: 13, fontFamily: 'Inter_700Bold' },
    copiedNotice: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 12 },
    copiedText: { color: colors.primary, fontSize: 11, fontFamily: 'Inter_500Medium' },
    tip: { backgroundColor: `${colors.accent}14`, borderRadius: 15, padding: 13, flexDirection: 'row', gap: 10, marginTop: 17 },
    tipIcon: { width: 22, height: 22, borderRadius: 8, alignItems: 'center', justifyContent: 'center', backgroundColor: `${colors.accent}18` },
    tipText: { flex: 1, color: colors.secondaryForeground, fontSize: 11, lineHeight: 17, fontFamily: 'Inter_400Regular' },
    activityCard: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.border, borderRadius: 19, paddingHorizontal: 14 },
    activityRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 14, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.border },
    activityAvatar: { width: 37, height: 37, borderRadius: 13, backgroundColor: colors.muted, alignItems: 'center', justifyContent: 'center' },
    activityInitial: { color: colors.primary, fontFamily: 'Inter_700Bold' },
    activityCopy: { flex: 1, marginLeft: 11 },
    activityTitle: { color: colors.foreground, fontSize: 12, fontFamily: 'Inter_700Bold' },
    activityDate: { color: colors.mutedForeground, fontSize: 10, fontFamily: 'Inter_400Regular', marginTop: 3 },
    activityReward: { color: colors.primary, fontSize: 11, fontFamily: 'Inter_700Bold', marginRight: 8 },
    activityCheck: { marginRight: 1 },
    pressed: { opacity: 0.78, transform: [{ scale: 0.98 }] },
  });
}