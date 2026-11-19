package com.infiniplay.core;
import java.util.*;

/** Merge fingers and hardware sources without letting one source release another's held button. */
public final class DigitalInput {
  public record Change(int key, boolean pressed) {}
  private final Map<String, Set<Integer>> sources = new HashMap<>();
  public List<Change> update(String source, Set<Integer> keys) {
    Set<Integer> before = held();
    if (keys.isEmpty())
      sources.remove(source);
    else
      sources.put(source, Set.copyOf(keys));
    Set<Integer> after = held();
    List<Change> changes = new ArrayList<>();
    for (int key : before)
      if (!after.contains(key))
        changes.add(new Change(key, false));
    for (int key : after)
      if (!before.contains(key))
        changes.add(new Change(key, true));
    return changes;
  }
  public Set<Integer> held() {
    Set<Integer> result = new HashSet<>();
    sources.values().forEach(result::addAll);
    return result;
  }
  public List<Change> removePrefix(String prefix) {
    List<Change> changes = new ArrayList<>();
    for (String source : new ArrayList<>(sources.keySet()))
      if (source.startsWith(prefix))
        changes.addAll(update(source, Set.of()));
    return changes;
  }
  public List<Change> clear() {
    return removePrefix("");
  }
}
