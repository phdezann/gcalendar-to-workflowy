package org.phdezann.cn.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DescriptionUpdaterTest {

    private final DescriptionUpdater descriptionUpdater = new DescriptionUpdater(new LinkParser());

    @Test
    void update_old_desc_empty() {
        var newDesc = descriptionUpdater.update("", "123", Optional.empty());

        assertThat(newDesc).isEqualTo("[workflowy:123]");
    }

    @Test
    void update_old_desc_not_empty() {
        var newDesc = descriptionUpdater.update("old desc", "123", Optional.empty());

        assertThat(newDesc).isEqualTo("old desc\n[workflowy:123]");
    }

}
