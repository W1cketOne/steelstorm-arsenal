package com.steelstorm.arsenal.client.boss;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * A compact way to write vanilla keyframe animations: each channel takes groups of four numbers,
 * {seconds, x, y, z}. Rotations are degrees added to the bone's rest pose, positions are pixels.
 */
final class Anim {
    private final AnimationDefinition.Builder builder;

    private Anim(float seconds) {
        builder = AnimationDefinition.Builder.withLength(seconds);
    }

    static Anim of(float seconds) {
        return new Anim(seconds);
    }

    Anim loop() {
        builder.looping();
        return this;
    }

    Anim rot(String bone, float... keys) {
        return channel(bone, AnimationChannel.Targets.ROTATION, keys, 0);
    }

    Anim pos(String bone, float... keys) {
        return channel(bone, AnimationChannel.Targets.POSITION, keys, 1);
    }

    Anim scale(String bone, float... keys) {
        return channel(bone, AnimationChannel.Targets.SCALE, keys, 2);
    }

    private Anim channel(String bone, AnimationChannel.Target target, float[] keys, int kind) {
        Keyframe[] frames = new Keyframe[keys.length / 4];
        for (int i = 0; i < frames.length; i++) {
            float t = keys[i * 4];
            float x = keys[i * 4 + 1];
            float y = keys[i * 4 + 2];
            float z = keys[i * 4 + 3];
            frames[i] = new Keyframe(t, switch (kind) {
                case 0 -> KeyframeAnimations.degreeVec(x, y, z);
                case 1 -> KeyframeAnimations.posVec(x, y, z);
                default -> KeyframeAnimations.scaleVec(x, y, z);
            }, AnimationChannel.Interpolations.CATMULLROM);
        }
        builder.addAnimation(bone, new AnimationChannel(target, frames));
        return this;
    }

    AnimationDefinition build() {
        return builder.build();
    }
}
