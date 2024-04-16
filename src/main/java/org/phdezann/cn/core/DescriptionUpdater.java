package org.phdezann.cn.core;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.phdezann.cn.core.LinkParser.WorkflowyLink;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DescriptionUpdater {

    private final LinkParser linkParser;

    public String update(String description, String bulletId, Optional<WorkflowyLink> link) {
        if (link.isPresent()) {
            description = StringUtils.remove(description, link.get().getWholeLink());
        }
        description = trimToEmpty(description);
        var separator = isEmpty(description) ? "" : "\n";
        return trimToEmpty(description) + separator + linkParser.buildLink(bulletId);
    }
}
