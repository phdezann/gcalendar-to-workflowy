package org.phdezann.cn.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NotePrefixLengthFinderTest {

    @Test
    void findPrefixLength() {
        assertThat(NotePrefixLengthFinder.findPrefixLength("🥼abc")).isEqualTo(5);
    }

    @Test
    void findPrefixLength_noEmoji() {
        assertThat(NotePrefixLengthFinder.findPrefixLength("abc")).isEqualTo(0);
    }

    @Test
    void findPrefixLength_emojiOnly() {
        assertThat(NotePrefixLengthFinder.findPrefixLength("🥼")).isEqualTo(5);
    }

    @Test
    void findPrefixLength_textOnly() {
        assertThat(NotePrefixLengthFinder.findPrefixLength("abc")).isEqualTo(0);
    }

}
