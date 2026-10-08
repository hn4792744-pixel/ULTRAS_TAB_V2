package me.uc.hussein.ultrastab.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A group of templates (e.g. a header) plus the union of their animations and dynamic flag. */
public record TemplateSet(List<Template> lines, Set<String> animations, boolean dynamic) {

    public static final TemplateSet EMPTY = new TemplateSet(List.of(), Set.of(), false);

    public static TemplateSet of(List<String> raw) {
        List<Template> lines = new ArrayList<>(raw.size());
        Set<String> animations = new HashSet<>();
        boolean dynamic = false;
        for (String line : raw) {
            Template t = Template.of(line);
            lines.add(t);
            animations.addAll(t.animations());
            dynamic |= t.dynamic();
        }
        return new TemplateSet(List.copyOf(lines), Set.copyOf(animations), dynamic);
    }
}
