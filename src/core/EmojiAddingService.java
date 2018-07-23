package core;

import api.StringList;
import api.StringListImpl;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import util.AlertService;

import javax.annotation.Nullable;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Deprecated // deprecated as of v2.3, use PersistenceManager instead
public class EmojiAddingService {

    private EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
    private AlertService alertService = new AlertService();

    @Deprecated
    public void addEmojis(List<String> emojis,
                          List<String> discordEmojis,
                          StringList randomTags,
                          @Nullable MessageChannel channel,
                          @Nullable Guild guild) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();

        List<String> addedEmojis = Lists.newArrayList();
        List<String> existingEmojis = Lists.newArrayList();
        List<String> adjustedEmojis = Lists.newArrayList();

        for (int i = 0; i < emojis.size(); i++) {
            String emoji = emojis.get(i);
            if (!emojiExists(doc, emoji)) {
                Element emojiElem = doc.createElement("emoji");
                emojiElem.setAttribute("value", emoji);
                if (randomTags.size() == 1) emojiElem.setAttribute("random", randomTags.get(0));
                if (randomTags.size() > 1) emojiElem.setAttribute("random", randomTags.tryGet(i));
                rootElem.appendChild(emojiElem);

                addedEmojis.add(emoji);
            } else {
                Element emojiElem = getEmojiElem(doc, emoji);
                String randomTag = emojiElem.getAttribute("random");
                if (!randomTags.isEmpty()) {
                    boolean random = randomTag.equals("") || Boolean.parseBoolean(randomTag);
                    boolean newRandom = randomTags.size() == 1 ? Boolean.parseBoolean(randomTags.get(0))
                        : Boolean.parseBoolean(randomTags.tryGet(i));
                    if (random == newRandom) {
                        existingEmojis.add(emoji);
                    } else {
                        emojiElem.setAttribute("random", Boolean.toString(newRandom));
                        adjustedEmojis.add(emoji);
                    }
                }
                existingEmojis.add(emoji);
            }
        }

        if (guild != null) {
            for (int i = 0; i < discordEmojis.size(); i++) {
                String emojiName = discordEmojis.get(i);
                for (Emote emote : guild.getEmotesByName(emojiName, true)) {
                    String emojiValue = emote.getAsMention();
                    if (!discordEmojiExists(doc, emojiValue)) {
                        Element emojiElem = doc.createElement("discord-emoji");
                        emojiElem.setAttribute("value", emojiValue);
                        if (randomTags.size() == 1) emojiElem.setAttribute("random", randomTags.get(0));
                        if (randomTags.size() > 1) emojiElem.setAttribute("random", randomTags.tryGet(i));
                        emojiElem.setAttribute("name", emojiName);
                        emojiElem.setAttribute("guildId", guild.getId());
                        emojiElem.setAttribute("guildName", guild.getName());
                        rootElem.appendChild(emojiElem);

                        addedEmojis.add(emojiValue);
                    } else {
                        Element emojiElem = getDiscordEmojiElem(doc, emojiValue);
                        String randomTag = emojiElem.getAttribute("random");
                        if (!randomTags.isEmpty()) {
                            boolean random = randomTag.equals("") || Boolean.parseBoolean(randomTag);
                            boolean newRandom = randomTags.size() == 1 ? Boolean.parseBoolean(randomTags.tryGet(0))
                                : Boolean.parseBoolean(randomTags.tryGet(i));
                            if (random == newRandom) {
                                existingEmojis.add(emojiValue);
                            } else {
                                emojiElem.setAttribute("random", Boolean.toString(newRandom));
                                adjustedEmojis.add(emojiValue);
                            }
                        }
                        existingEmojis.add(emojiValue);
                    }
                }
            }
        }

        writeToFile(doc);
        alertService.alertAddedEmojis(addedEmojis, existingEmojis, adjustedEmojis, channel);
    }

    @Deprecated
    public void addEmojis(List<String> emojis,
                          List<String> discordEmojis,
                          StringList randomTags,
                          String[] keywords,
                          String[] replaceTags,
                          @Nullable MessageChannel channel,
                          @Nullable Guild guild) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();

        List<String> addedEmojis = Lists.newArrayList();
        List<String> existingEmojis = Lists.newArrayList();
        List<String> adjustedEmojis = Lists.newArrayList();
        Multimap<String, String> addedKeywords = HashMultimap.create();
        Multimap<String, String> existingKeywords = HashMultimap.create();
        Multimap<String, String> adjustedKeywords = HashMultimap.create();

        for (int i = 0; i < emojis.size(); i++) {
            String emoji = emojis.get(i);
            Element emojiElem;

            //only create new emoji if there isn't one with the same value, else load the existing emoji
            if (!emojiExists(doc, emoji)) {
                emojiElem = doc.createElement("emoji");
                emojiElem.setAttribute("value", emoji);
                if (randomTags.size() == 1) emojiElem.setAttribute("random", randomTags.get(0));
                if (randomTags.size() > 1) emojiElem.setAttribute("random", randomTags.tryGet(i));

                addedEmojis.add(emoji);
            } else {
                emojiElem = getEmojiElem(doc, emoji);
                String randomTag = emojiElem.getAttribute("random");
                if (!randomTags.isEmpty()) {
                    boolean random = randomTag.equals("") || Boolean.parseBoolean(randomTag);
                    boolean newRandom = randomTags.size() == 1 ? Boolean.parseBoolean(randomTags.get(0))
                        : Boolean.parseBoolean(randomTags.tryGet(i));
                    if (random == newRandom) {
                        existingEmojis.add(emoji);
                    } else {
                        emojiElem.setAttribute("random", Boolean.toString(newRandom));
                        adjustedEmojis.add(emoji);
                    }
                }
                existingEmojis.add(emoji);
            }

            for (int j = 0; j < keywords.length; j++) {
                Element keywordElem;

                //only create new keyword if it doesn't already exist on the same emoji, else load existing keyword
                // and adjust replace flag
                if (!keywordExists(emojiElem, keywords[j])) {
                    keywordElem = doc.createElement("keyword");
                    keywordElem.setAttribute("replace", replaceTags[j]);
                    keywordElem.setTextContent(keywords[j]);

                    addedKeywords.put(emoji, keywords[j]);
                } else {
                    keywordElem = getKeywordElem(emojiElem, keywords[j]);

                    if (!keywordElem.getAttribute("replace").equals(replaceTags[j])) {
                        keywordElem.setAttribute("replace", replaceTags[j]);

                        adjustedKeywords.put(emoji, keywords[j]);
                    } else {
                        existingKeywords.put(emoji, keywords[j]);
                    }
                }

                emojiElem.appendChild(keywordElem);
            }

            rootElem.appendChild(emojiElem);
        }

        if (guild != null) {
            for (int i = 0; i < discordEmojis.size(); i++) {
                String emojiName = discordEmojis.get(i);
                for (Emote emote : guild.getEmotesByName(emojiName, true)) {
                    String emojiValue = emote.getAsMention();
                    Element emojiElem;

                    if (!discordEmojiExists(doc, emojiValue)) {
                        emojiElem = doc.createElement("discord-emoji");
                        emojiElem.setAttribute("value", emojiValue);
                        if (randomTags.size() == 1) emojiElem.setAttribute("random", randomTags.get(0));
                        if (randomTags.size() > 1) emojiElem.setAttribute("random", randomTags.tryGet(i));
                        emojiElem.setAttribute("name", emojiName);
                        emojiElem.setAttribute("guildId", guild.getId());
                        emojiElem.setAttribute("guildName", guild.getName());

                        addedEmojis.add(emojiValue);
                    } else {
                        emojiElem = getDiscordEmojiElem(doc, emojiValue);
                        String randomTag = emojiElem.getAttribute("random");
                        if (!randomTags.isEmpty()) {
                            boolean random = randomTag.equals("") || Boolean.parseBoolean(randomTag);
                            boolean newRandom = randomTags.size() == 1 ? Boolean.parseBoolean(randomTags.get(0))
                                : Boolean.parseBoolean(randomTags.tryGet(i));
                            if (random == newRandom) {
                                existingEmojis.add(emojiValue);
                            } else {
                                emojiElem.setAttribute("random", Boolean.toString(newRandom));
                                adjustedEmojis.add(emojiValue);
                            }
                        }
                        existingEmojis.add(emojiValue);
                    }

                    for (int j = 0; j < keywords.length; j++) {
                        Element keywordElem;

                        if (!keywordExists(emojiElem, keywords[j])) {
                            keywordElem = doc.createElement("keyword");
                            keywordElem.setAttribute("replace", replaceTags[j]);
                            keywordElem.setTextContent(keywords[j]);

                            addedKeywords.put(emojiValue, keywords[j]);
                        } else {
                            keywordElem = getKeywordElem(emojiElem, keywords[j]);

                            if (!keywordElem.getAttribute("replace").equals(replaceTags[j])) {
                                keywordElem.setAttribute("replace", replaceTags[j]);

                                adjustedKeywords.put(emojiValue, keywords[j]);
                            } else {
                                existingKeywords.put(emojiValue, keywords[j]);
                            }
                        }

                        emojiElem.appendChild(keywordElem);
                    }

                    rootElem.appendChild(emojiElem);
                }
            }
        }

        writeToFile(doc);
        alertService.alertAddedEmojis(addedEmojis, existingEmojis, adjustedEmojis, addedKeywords, adjustedKeywords, existingKeywords, channel);
    }

    @Deprecated
    public void removeEmojis(List<String> emojisToRemove,
                             List<String> discordEmojisToRemove,
                             @Nullable MessageChannel channel,
                             @Nullable Guild guild) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();
        List<Element> emojiElems = getEmojiElems(doc, emojisToRemove);
        List<String> removedEmojis = Lists.newArrayList();
        List<String> emojistToRemoveValues = Lists.newArrayList();

        for (Element emoji : emojiElems) {
            String emojiValue = emoji.getAttribute("value");

            if (emojisToRemove.contains(emojiValue)) {
                rootElem.removeChild(emoji);

                emojisToRemove.remove(emojiValue);
                removedEmojis.add(emojiValue);
            }
        }

        if (guild != null) {
            emojistToRemoveValues = getDiscordEmojiValues(discordEmojisToRemove, guild);
            for (Element element : getDiscordEmojiElems(doc, emojistToRemoveValues)) {
                String value = element.getAttribute("value");
                rootElem.removeChild(element);
                removedEmojis.add(value);
                emojistToRemoveValues.remove(value);
            }
        }

        writeToFile(doc);
        alertService.alertRemovedEmojis(removedEmojis, StringListImpl.join(emojisToRemove, emojistToRemoveValues).getValues(), channel);
    }

    @Deprecated
    public void removeKeywords(List<String> emojis,
                               List<String> discordEmojis,
                               List<String> keywordsToRemove,
                               @Nullable MessageChannel channel,
                               @Nullable Guild guild) {
        Document doc = emojiLoadingService.getDocument();
        List<Element> emojiElems = getEmojiElems(doc, emojis);
        List<String> discordEmojiValues = Lists.newArrayList();
        Multimap<String, String> removedKeywords = HashMultimap.create();
        Multimap<String, String> missingKeywords = HashMultimap.create();

        for (Element emoji : emojiElems) {
            String emojiValue = emoji.getAttribute("value");

            if (emojis.contains(emojiValue)) {
                List<Element> keywordElems = getKeywordElems(emoji, keywordsToRemove);

                List<String> keywordsOnEmoji = Lists.newArrayList(keywordsToRemove);
                for (Element keyword : keywordElems) {
                    String keywordValue = keyword.getTextContent();

                    if (keywordsToRemove.contains(keywordValue)) {
                        emoji.removeChild(keyword);

                        keywordsOnEmoji.remove(keywordValue);
                        removedKeywords.put(emojiValue, keywordValue);
                    }
                }

                //if keywordsOnEmoji is not empty that means not all selected keywords have been found on this emoji
                if (!keywordsOnEmoji.isEmpty()) {
                    keywordsOnEmoji.forEach(k -> missingKeywords.put(emojiValue, k));
                }
            }

            emojis.remove(emojiValue);
        }

        if (guild != null) {
            discordEmojiValues = getDiscordEmojiValues(discordEmojis, guild);
            for (Element emojiElem : getDiscordEmojiElems(doc, discordEmojiValues)) {
                String emojiName = emojiElem.getAttribute("name");
                String emojiValue = emojiElem.getAttribute("value");

                if (discordEmojis.contains(emojiName)) {
                    List<Element> keywordElems = getKeywordElems(emojiElem, keywordsToRemove);
                    List<String> keywordsOnEmoji = Lists.newArrayList(keywordsToRemove);

                    for (Element keywordElem : keywordElems) {
                        String keyword = keywordElem.getTextContent();

                        if (keywordsToRemove.contains(keyword)) {
                            emojiElem.removeChild(keywordElem);

                            keywordsOnEmoji.remove(keyword);
                            removedKeywords.put(emojiValue, keyword);
                        }
                    }

                    if (!keywordsOnEmoji.isEmpty()) {
                        keywordsOnEmoji.forEach(k -> missingKeywords.put(emojiValue, k));
                    }
                }

                discordEmojiValues.remove(emojiValue);
            }
        }

        writeToFile(doc);
        alertService.alertRemovedKeywords(StringListImpl.join(emojis, discordEmojiValues).getValues(), removedKeywords, missingKeywords, channel);
    }

    @Deprecated
    public void mergeDuplicateEmojis(Set<String> duplicateEmojis, @Nullable MessageChannel channel) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();

        NodeList emojis = doc.getElementsByTagName("emoji");
        List<Element> newEmojis = Lists.newArrayList();
        List<String> mergedEmojis = Lists.newArrayList();

        //create new emoji elements for each duplicate emoji
        for (String duplicateEmoji : duplicateEmojis) {
            Element emoji = doc.createElement("emoji");
            emoji.setAttribute("value", duplicateEmoji);

            newEmojis.add(emoji);
        }

        for (Element newEmoji : newEmojis) {
            String emojiValue = newEmoji.getAttribute("value");
            List<Element> otherEmojis = Lists.newArrayList();
            List<Element> keywords = Lists.newArrayList();

            //get other emojis with the same value
            for (int i = 0; i < emojis.getLength(); i++) {
                Element emojiElem = (Element) emojis.item(i);

                if (emojiElem.getAttribute("value").equals(emojiValue)) {
                    otherEmojis.add(emojiElem);
                }
            }

            //set random attribute, false if all of them are false
            Boolean random = otherEmojis.stream()
                    .anyMatch(e -> e.getAttribute("random").equals("")
                            || Boolean.parseBoolean(e.getAttribute("random")));
            newEmoji.setAttribute("random", random.toString());

            //get keywords of those emojis and remove them after
            for (Element otherEmoji : otherEmojis) {
                NodeList keywordElems = otherEmoji.getElementsByTagName("keyword");

                for (int i = 0; i < keywordElems.getLength(); i++) {
                    keywords.add((Element) keywordElems.item(i));
                }

                rootElem.removeChild(otherEmoji);
            }

            //add keywords of the other emojis of the same value
            for (Element keyword : keywords) {
                newEmoji.appendChild(keyword);
            }

            rootElem.appendChild(newEmoji);
            mergedEmojis.add(emojiValue);
        }

        writeToFile(doc);
        alertService.alertMergedEmojis(mergedEmojis, channel);
    }

    @Deprecated
    public void mergeDuplicateKeywords(Multimap<String, String> emojisWithDuplicateKeywords, @Nullable MessageChannel channel) {
        Document doc = emojiLoadingService.getDocument();

        List<Element> emojis = nodeListToElementList(doc.getElementsByTagName("emoji"));
        Multimap<String, String> mergedKeywords = HashMultimap.create();

        for (String emoji : emojisWithDuplicateKeywords.keys()) {
            //load the emoji element for the current emoji for which we want to merge duplicate keywords
            List<Element> emojiElems = emojis.stream().filter(e -> e.getAttribute("value").equals(emoji))
                    .collect(Collectors.toList());
            if (emojiElems.size() == 1) {
                Element emojiElem = emojiElems.get(0);
                List<Element> keywordElems = nodeListToElementList(emojiElem.getElementsByTagName("keyword"));

                //loop over the different duplicate keyword values on current emoji
                for (String keyword : emojisWithDuplicateKeywords.get(emoji)) {
                    //load all duplicates for the current keyword
                    List<Element> selectedKeywords = keywordElems.stream().filter(k -> k.getTextContent().equals(keyword))
                            .collect(Collectors.toList());
                    //set replace = true for the new keyword if all keywords are true
                    Boolean isReplace = selectedKeywords.stream().allMatch(k -> Boolean.parseBoolean(k.getAttribute("replace")));

                    //remove duplicates
                    for (Element keywordElem : selectedKeywords) {
                        emojiElem.removeChild(keywordElem);
                    }

                    //create and add new keyword
                    Element newKeyword = doc.createElement("keyword");
                    newKeyword.setAttribute("replace", isReplace.toString());
                    newKeyword.setTextContent(keyword);
                    emojiElem.appendChild(newKeyword);

                    mergedKeywords.put(emoji, keyword);
                }
            } else if (emojiElems.size() > 1) {
                throw new IllegalStateException("More than one emoji found for value " + emoji + ". Keyword cleaning failed.");
            } else {
                throw new IllegalStateException("No emoji found for value " + emoji + ". Keyword cleaning failed.");
            }
        }

        writeToFile(doc);
        alertService.alertMergedKeywords(mergedKeywords, channel);
    }

    @Deprecated
    public void setKeywordsToLowerCase(@Nullable MessageChannel channel) {
        Document doc = emojiLoadingService.getDocument();
        List<Element> keywordElems = nodeListToElementList(doc.getElementsByTagName("keyword"));
        List<String> changedKeywords = Lists.newArrayList();

        for (Element keywordElem : keywordElems) {
            String keyword = keywordElem.getTextContent();

            if (!keyword.equals(keyword.toLowerCase())) {
                keywordElem.setTextContent(keyword.toLowerCase());

                changedKeywords.add(keyword);
            }
        }

        writeToFile(doc);
        alertService.alertUpperCaseKeywords(changedKeywords, channel);
    }

    private boolean emojiExists(Document doc, String emojiString) {
        return getEmojiElem(doc, emojiString) != null;
    }

    private boolean discordEmojiExists(Document doc, String emojiValue) {
        try {
            return getDiscordEmojiElem(doc, emojiValue) != null;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return true;
        }
    }

    private boolean keywordExists(Element emojiElem, String keywordString) {
        return getKeywordElem(emojiElem, keywordString) != null;
    }

    private List<Element> getEmojiElems(Document doc, List<String> emojiStrings) {
        List<Element> elems = Lists.newArrayList();
        NodeList emojiList = doc.getElementsByTagName("emoji");

        for (int i = 0; i < emojiList.getLength(); i++) {
            Element emoji = (Element) emojiList.item(i);
            String emojiValue = emoji.getAttribute("value");

            if (emojiStrings.contains(emojiValue)) {
                elems.add(emoji);
            }
        }

        return elems;
    }

    private Element getEmojiElem(Document doc, String emojiString) {
        NodeList emojiList = doc.getElementsByTagName("emoji");

        for (int i = 0; i < emojiList.getLength(); i++) {
            Element emoji = (Element) emojiList.item(i);
            String emojiValue = emoji.getAttribute("value");

            if (emojiString.equals(emojiValue)) {
                return emoji;
            }
        }

        return null;
    }

    private List<Element> getDiscordEmojiElems(Document doc, List<String> emojiValues) {
        List<Element> emojiElems = nodeListToElementList(doc.getElementsByTagName("discord-emoji"));

        return emojiElems.stream()
                .filter(e -> emojiValues.contains(e.getAttribute("value")))
                .collect(Collectors.toList());
    }

    private Element getDiscordEmojiElem(Document doc, String emojiValue) {
        List<Element> emojiElems = nodeListToElementList(doc.getElementsByTagName("discord-emoji"));

        List<Element> matchedEmojis = emojiElems.stream()
                .filter(e -> e.getAttribute("value").equals(emojiValue))
                .collect(Collectors.toList());

        if (matchedEmojis.size() == 1) {
            return matchedEmojis.get(0);
        } else if (matchedEmojis.size() > 1) {
            throw new IllegalStateException("Duplicate Discord Emojis found. Clean up your XML configuration using the clean command");
        }

        return null;
    }

    private List<Element> getKeywordElems(Element emojiElem, List<String> keywordStrings) {
        List<Element> elems = Lists.newArrayList();
        NodeList keywordList = emojiElem.getElementsByTagName("keyword");

        for (int i = 0; i < keywordList.getLength(); i++) {
            Element keyword = (Element) keywordList.item(i);
            String keywordValue = keyword.getTextContent();

            if (keywordStrings.contains(keywordValue)) {
                elems.add(keyword);
            }
        }

        return elems;
    }

    private Element getKeywordElem(Element emojiElem, String keywordString) {
        NodeList keywordList = emojiElem.getElementsByTagName("keyword");

        for (int i = 0; i < keywordList.getLength(); i++) {
            Element keyword = (Element) keywordList.item(i);
            String keywordValue = keyword.getTextContent();

            if (keywordValue.equals(keywordString)) {
                return keyword;
            }
        }

        return null;
    }

    private List<Element> nodeListToElementList(NodeList nodeList) {
        List<Element> elements = Lists.newArrayList();
        for (int i = 0; i < nodeList.getLength(); i++) {
            elements.add((Element) nodeList.item(i));
        }

        return elements;
    }

    private List<String> getDiscordEmojiValues(List<String> names, Guild guild) {
        List<String> discordEmojiValues = Lists.newArrayList();

        for (String emoji : names) {
            discordEmojiValues.addAll(
                    guild.getEmotesByName(emoji, true).stream()
                            .map(Emote::getAsMention)
                            .collect(Collectors.toList())
            );
        }

        return discordEmojiValues;
    }

    private void writeToFile(Document doc) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("./resources/emojis.xml"));

            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

}
