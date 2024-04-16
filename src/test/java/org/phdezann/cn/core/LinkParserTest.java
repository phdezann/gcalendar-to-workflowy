package org.phdezann.cn.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LinkParserTest {

    private final LinkParser linkParser = new LinkParser();

    @Test
    void extractWorkflowyLink() {
        var createdLink = linkParser.buildLink("1667de14-9d20-44ac-836d-c11d6723cc87");
        var extractedLink = linkParser.extractWorkflowyLink("ABC" + createdLink + "DEF");

        assertThat(extractedLink).hasValueSatisfying(value -> //
        assertThat(value.getBulletId()).isEqualTo("c11d6723cc87"));
    }

}
