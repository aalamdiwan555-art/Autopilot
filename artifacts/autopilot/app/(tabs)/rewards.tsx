import { Feather } from '@expo/vector-icons';
import * as Haptics from 'expo-haptics';
import React, { useState } from 'react';
import { Platform, Pressable, ScrollView, Share, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import colors from '@/constants/colors';

export default function RewardsScreen() {
  const insets = useSafeAreaInsets();
  const [copied, setCopied] = useState(false);
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
    <ScrollView style={styles.screen} contentContainerStyle={[styles.content, { paddingTop: Math.max(insets.top, 18), paddingBottom: insets.bottom + 100 }]} showsVerticalScrollIndicator={false}>
      <Text style={styles.eyebrow}>YOUR PERKS</Text><Text style={styles.title}>Refer & earn</Text>
      <Text style={styles.subtitle}>Bring a driver friend on board. You both get seven days of Autopilot Pro.</Text>
      <View style={styles.rewardCard}><View style={styles.rewardIcon}><Feather name="gift" size={25} color={colors.light.primaryForeground} /></View><Text style={styles.rewardValue}>7 days</Text><Text style={styles.rewardLabel}>earned for every friend who starts a shift</Text><View style={styles.divider} /><View style={styles.rewardBottom}><View><Text style={styles.smallLabel}>TOTAL EARNED</Text><Text style={styles.smallValue}>14 days</Text></View><View><Text style={styles.smallLabel}>FRIENDS JOINED</Text><Text style={styles.smallValue}>2</Text></View></View></View>
      <Text style={styles.sectionTitle}>Your invite code</Text>
      <View style={styles.codeCard}><View><Text style={styles.codeLabel}>SHARE THIS CODE</Text><Text style={styles.code}>ARUN-7K2P</Text></View><Pressable testID="copy-code" onPress={copyCode} style={({ pressed }) => [styles.copyButton, pressed && styles.pressed]}><Feather name={copied ? 'check' : 'copy'} size={17} color={colors.light.primaryForeground} /></Pressable></View>
      <Pressable testID="share-referral" onPress={share} style={({ pressed }) => [styles.shareButton, pressed && styles.pressed]}><Feather name="send" size={17} color={colors.light.primaryForeground} /><Text style={styles.shareText}>Share with a friend</Text></Pressable>
      <View style={styles.tip}><Feather name="info" size={17} color={colors.light.accent} /><Text style={styles.tipText}>Your reward is added automatically after your friend completes their first active shift.</Text></View>
      <Text style={styles.sectionTitle}>Recent rewards</Text>
      <View style={styles.activityCard}><View style={styles.activityRow}><View style={styles.activityAvatar}><Text style={styles.activityInitial}>S</Text></View><View style={styles.activityCopy}><Text style={styles.activityTitle}>Sanjay joined Autopilot</Text><Text style={styles.activityDate}>Aug 18 · reward unlocked</Text></View><Text style={styles.activityReward}>+7 days</Text></View><View style={styles.activityRow}><View style={styles.activityAvatar}><Text style={styles.activityInitial}>R</Text></View><View style={styles.activityCopy}><Text style={styles.activityTitle}>Rahul joined Autopilot</Text><Text style={styles.activityDate}>Aug 12 · reward unlocked</Text></View><Text style={styles.activityReward}>+7 days</Text></View></View>
    </ScrollView>
  );
}
const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.light.background }, content: { paddingHorizontal: 20 },
  eyebrow: { color: colors.light.primary, fontSize: 11, letterSpacing: 1.5, fontFamily: 'Inter_700Bold' }, title: { color: colors.light.foreground, fontSize: 30, fontFamily: 'Inter_700Bold', marginTop: 7 }, subtitle: { color: colors.light.mutedForeground, fontSize: 14, lineHeight: 21, fontFamily: 'Inter_400Regular', marginTop: 8, maxWidth: 330 },
  rewardCard: { backgroundColor: colors.light.secondary, borderRadius: 26, padding: 20, marginTop: 24, overflow: 'hidden' }, rewardIcon: { width: 52, height: 52, borderRadius: 18, backgroundColor: colors.light.primary, alignItems: 'center', justifyContent: 'center' }, rewardValue: { color: colors.light.foreground, fontSize: 38, fontFamily: 'Inter_700Bold', marginTop: 24 }, rewardLabel: { color: colors.light.secondaryForeground, fontSize: 13, fontFamily: 'Inter_500Medium', marginTop: 3 }, divider: { height: 1, backgroundColor: colors.light.border, marginVertical: 20 }, rewardBottom: { flexDirection: 'row', gap: 56 }, smallLabel: { color: colors.light.mutedForeground, fontSize: 9, letterSpacing: 1, fontFamily: 'Inter_700Bold' }, smallValue: { color: colors.light.foreground, fontSize: 16, fontFamily: 'Inter_700Bold', marginTop: 7 },
  sectionTitle: { color: colors.light.foreground, fontSize: 17, fontFamily: 'Inter_700Bold', marginTop: 27, marginBottom: 11 }, codeCard: { backgroundColor: colors.light.card, borderWidth: 1, borderColor: colors.light.border, borderRadius: 20, padding: 16, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }, codeLabel: { color: colors.light.mutedForeground, fontSize: 9, letterSpacing: 1, fontFamily: 'Inter_700Bold' }, code: { color: colors.light.primary, fontSize: 23, letterSpacing: 1.5, fontFamily: 'Inter_700Bold', marginTop: 7 }, copyButton: { width: 44, height: 44, borderRadius: 15, backgroundColor: colors.light.primary, alignItems: 'center', justifyContent: 'center' }, shareButton: { height: 54, borderRadius: 17, backgroundColor: colors.light.primary, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8, marginTop: 11 }, shareText: { color: colors.light.primaryForeground, fontSize: 14, fontFamily: 'Inter_700Bold' }, tip: { backgroundColor: '#F5B97D1C', borderRadius: 16, padding: 14, flexDirection: 'row', gap: 10, marginTop: 16 }, tipText: { flex: 1, color: colors.light.secondaryForeground, fontSize: 12, lineHeight: 18, fontFamily: 'Inter_400Regular' }, activityCard: { backgroundColor: colors.light.card, borderWidth: 1, borderColor: colors.light.border, borderRadius: 20, paddingHorizontal: 15 }, activityRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 14 }, activityAvatar: { width: 38, height: 38, borderRadius: 14, backgroundColor: colors.light.muted, alignItems: 'center', justifyContent: 'center' }, activityInitial: { color: colors.light.primary, fontFamily: 'Inter_700Bold' }, activityCopy: { flex: 1, marginLeft: 11 }, activityTitle: { color: colors.light.foreground, fontSize: 13, fontFamily: 'Inter_700Bold' }, activityDate: { color: colors.light.mutedForeground, fontSize: 11, fontFamily: 'Inter_400Regular' }, activityReward: { color: colors.light.primary, fontSize: 12, fontFamily: 'Inter_700Bold' }, pressed: { opacity: 0.78, transform: [{ scale: 0.98 }] },
});