function initContentEditor(containerId) {
    var container = document.getElementById(containerId);

    var previousValue = container.innerHTML;
    var threshold = 500;
    // TODO: allow setting the threshold
    var checkContentEditorChange = function() {
        var currentValue = container.innerHTML;
        if (currentValue != previousValue) {
            groupedSend(containerId, threshold, 'contentChanged', containerId, {
                html: currentValue
            });
            previousValue = currentValue;
        }
    };
    container.addEventListener('input', checkContentEditorChange);
    container.addEventListener('styleChanged', checkContentEditorChange);
    container.addEventListener('manipulatedSelection', checkContentEditorChange);
}

/**
 * Toggles the style value state on the selection.
 *
 * @param containerId defines an optional container that the selection must be within to apply the style change.
 * @param styleName the css key camel-case.
 * @param styleValues the list of aliases for the value to match on. The first is what is set.
 * @param disabled the value to set if the value is already set on the entire selection.
 * @param nodes the list of nodes (will query if not provided).
 */
function toggleStyle(containerId, styleName, styleValues, disabled, nodes) {
    if (containerId != null) {
        if (!selectionInContainer(containerId)) return;       // Don't allow style toggling if outside the container
    }
    if (nodes == null) {
        nodes = selectedNodes();
    }
    var hasStyle = selectionHasStyle(styleName, styleValues, nodes);
    var styleValue = styleValues[0];
    if (hasStyle) {
        styleValue = disabled;
    }
    setStyle(null, styleName, styleValue);
}

function setStyle(containerId, styleName, styleValue, nodes) {
    if (containerId != null) {
        if (!selectionInContainer(containerId)) return;       // Don't allow styling if outside the container
    }
    if (nodes == null) {
        nodes = selectedNodes();
    }
    var spans = spansForSelected(nodes);
    for (var i = 0; i < spans.length; i++) {
        spans[i].style[styleName] = styleValue;
    }

    // -100
    // Re-select
    if (spans != null && spans.length > 0) {
        var range = rangy.createRange();
        range.setStartBefore(spans[0]);
        range.setEndAfter(spans[spans.length - 1]);
        var s = rangy.getSelection();
        s.removeAllRanges();
        s.addRange(range);

        // Fire event
        editorFor(range.commonAncestorContainer).dispatchEvent(new CustomEvent('styleChanged'));
    }
}

function adjustStyle(containerId, styleName, adjuster, nodes) {
    if (containerId != null) {
        if (!selectionInContainer(containerId)) return;       // Don't allow styling if outside the container
    }
    if (nodes == null) {
        nodes = selectedNodes();
    }
    var spans = spansForSelected(nodes);
    for (var i = 0; i < spans.length; i++) {
        spans[i].style[styleName] = adjuster(styleForNode(spans[i], styleName));
    }

    // Re-select
    if (spans != null && spans.length > 0) {
        var range = rangy.createRange();
        range.setStartBefore(spans[0]);
        range.setEndAfter(spans[spans.length - 1]);
        var s = rangy.getSelection();
        s.removeAllRanges();
        s.addRange(range);

        // Fire event
        editorFor(range.commonAncestorContainer).dispatchEvent(new CustomEvent('styleChanged'));
    }
}

function manipulateSelection(containerId, manipulator, nodes) {
    if (containerId != null) {
        if (!selectionInContainer(containerId)) return;       // Don't allow styling if outside the container
    }
    if (nodes == null) {
        nodes = selectedNodes();
    }
    var spans = spansForSelected(nodes);
    manipulator(spans);

    // Fire event
    editorFor(range.commonAncestorContainer).dispatchEvent(new CustomEvent('manipulatedSelection'));
}

function selectionInContainer(containerId) {
    var range = rangy.getSelection().getRangeAt(0);
    var container = document.getElementById(containerId);
    var element = range.commonAncestorContainer;
    while (element != null) {
        if (element == container) return true;
        element = element.parentNode;
    }
    return false;
}

function editorFor(container) {
    if (container.contentEditable == 'true') {
        return container;
    }
    return editorFor(container.parentNode);
}

/**
 * Checks to see if the entire selection has the style value applied.
 *
 * @param styleName the style name in camel-case
 * @param styleValues an array of all the style values that are considered a match (ex. ['bold', '700'])
 * @param nodes optional array of selectedNodes. If this is null it will get the selectedNodes.
 * @returns {boolean} true only if the entire selection has the style value applied.
 */
function selectionHasStyle(styleName, styleValues, nodes) {
    if (nodes == null) {
        nodes = selectedNodes();
    }

    var valuesString = styleValues.join(', ').toLowerCase();
    var value = selectionStyle(styleName, nodes);
    return valuesString.indexOf(value) != -1;
}

function styleForNode(node, styleName) {
    if (node.nodeType == Node.TEXT_NODE) {
        return window.getComputedStyle(node.parentNode, null)[styleName];
    } else {
        return window.getComputedStyle(node, null)[styleName];
    }
}

/**
 * Retrieves the styleValue for the entire selection. Will return null if the style is not consistent across the entire
 * selection.
 *
 * @param styleName the style name to look up.
 * @param nodes optionally specify the nodes array (will be selected if not provided).
 * @returns {string} the current style value or null
 */
function selectionStyle(styleName, nodes) {
    if (nodes == null) {
        nodes = selectedNodes();
    }
    var value = '----';
    for (var i = 0; i < nodes.length; i++) {
        var computed = window.getComputedStyle(nodes[i].node.parentNode, null)[styleName];
        computed = computed.toLowerCase();
        if (value == '----') {
            value = computed;
        } else if (computed != value) {
            value = null;
            break;
        }
    }
    if (value == '----') {
        value = null;
    }
    return value;
}

/**
 * Iterates over the supplied nodes (or gets them from selectedNodes if null) and makes sure every text node is within a
 * stylized span and properly breaks partial text blocks.
 *
 * @param nodes the nodes array from selectedNodes (will query itself if it is null).
 * @returns {Array} array of spans representing the current selection
 */
function spansForSelected(nodes) {
    if (nodes == null) {
        nodes = selectedNodes();
    }

    function createNewSpan(text, currentParent) {
        var span = document.createElement('span');
        var $span = $(span);
        $span.addClass('stylized');
        if (text != null) {
            span.appendChild(document.createTextNode(text));
        }
        if (currentParent != null) {
            span.style.cssText = currentParent.style.cssText;
        }
        return span;
    }

    function appendAfter(before, after) {
        if (before.nextSibling != null) {
            before.parentNode.insertBefore(after, before.nextSibling);
        } else {
            before.parentNode.appendChild(after);
        }
    }

    // Break out spans
    var spans = [];
    for (var i = 0; i < nodes.length; i++) {
        var nodeEntry = nodes[i];
        var node = nodeEntry.node;

        var parentStylized = null;
        if ($(node.parentNode).hasClass('stylized')) {
            parentStylized = node.parentNode;
        }
        if (parentStylized != null && nodeEntry.start == 0 && nodeEntry.end == nodeEntry.length) {
            spans.push(parentStylized);         // Already fully encapsulated
        } else {
            var preSpan = null;
            var postSpan = null;
            var emptyBlock = document.createTextNode('');
            var currentNode = node;
            if (parentStylized != null) {
                currentNode = parentStylized;
            }
            currentNode.parentNode.replaceChild(emptyBlock, currentNode);
            var previous = emptyBlock;
            if (nodeEntry.start > 0) {
                var preText = node.nodeValue.substring(0, nodeEntry.start);
                preSpan = createNewSpan(preText, parentStylized);
            }
            var newNode = createNewSpan(nodeEntry.text, parentStylized);
            spans.push(newNode);
            if (nodeEntry.end != nodeEntry.length) {
                var postText = node.nodeValue.substring(nodeEntry.end);
                postSpan = createNewSpan(postText, parentStylized);
            }
            if (preSpan != null) {
                appendAfter(previous, preSpan);
                previous = preSpan;
            }
            appendAfter(previous, newNode);
            previous = newNode;
            if (postSpan != null) {
                appendAfter(previous, postSpan);
                previous = postSpan;
            }
        }
    }
    return spans;
}

/**
 * Gathers all the text nodes for the current selection range and returns them in a JSON format:
 * { node, start, end, length, text }
 *
 * @returns {Array}
 */
function selectedNodes() {
    var range = rangy.getSelection().getRangeAt(0);
    var nodes = [];
    if (range.collapsed || range.endContainer == null) {
        var length = 0;
        if (range.startContainer.nodeType == Node.TEXT_NODE) {
            length = range.startContainer.nodeValue.length;
        } else {
            length = range.startContainer.childNodes.length;
        }
        nodes.push({
            node: range.startContainer,
            start: range.startOffset,
            end: range.endOffset,
            length: length
        });
    } else {
        var startContainer = range.startContainer;
        var startOffset = range.startOffset;
        var endContainer = range.endContainer;
        var endOffset = range.endOffset;
        if (startContainer == endContainer && startContainer.nodeType != Node.TEXT_NODE) {
            startContainer = startContainer.childNodes.item(startOffset);
            endContainer = startContainer.childNodes.item(endOffset);
            startOffset = 0;
            endOffset = 0;
        }
        var node = startContainer;
        while(true) {
            var start = 0;
            var length = 0;
            if (node.nodeType == Node.TEXT_NODE) {
                length = node.nodeValue.length;
            } else {
                length = node.childNodes.length;
            }
            var end = length;
            if (node == startContainer) {
                start = startOffset;
            }
            if (node == endContainer) {
                end = endOffset;
            }
            if (node.nodeType == Node.TEXT_NODE) {
                if (end - start > 0) {
                    var t = {
                        node: node,
                        start: start,
                        end: end,
                        length: length,
                        text: node.nodeValue.substring(start, end)
                    };
                    nodes.push(t);
//                    console.log('text: ' + t.text + ' (' + t.start + ' - ' + t.end + ')');
                }
            } else {
//                console.log('skipping container: ' + node + ' ' + start + ' - ' + end + ' (' + length + ') ' + node.childNodes.item(start));
            }


            if (node != endContainer) {
                if (node.hasChildNodes()) {
                    node = node.childNodes.item(start);
                } else if (node.nextSibling != null) {
                    node = node.nextSibling;
                } else {
                    node = node.parentNode.nextSibling;
                }
            } else {
                break;
            }
        }
    }
    return nodes;
}

/**
 * Insert an image into the range.
 *
 * @param containerId defines an optional container that the selection must be within to insert the image.
 * @param source the image source
 */
function insertImage(containerId, source) {
    var img = document.createElement('img');
    img.src = source;
    insertNode(containerId, img);
}

function insertNode(containerId, node) {
    if (containerId != null) {
        if (!selectionInContainer(containerId)) return;       // Don't allow if outside the container
    }
    var range = rangy.getSelection().getRangeAt(0);
    range.deleteContents();
    range.insertNode(node);
}

function insertIntoSelection(containerId, html) {
    var node = $.parseHTML(html)[0];
    insertNode(containerId, node);
}

function insertAroundSelection(containerId, html) {
    if (containerId != null) {
        if (!selectionInContainer(containerId)) return;       // Don't allow if outside the container
    }
    var range = rangy.getSelection().getRangeAt(0);
    if (range.canSurroundContents()) {
        var node = $.parseHTML(html)[0];
        range.surroundContents(node);
    }
}

/**
 * Adds a listener to detect when the style name value changes for the selection on the supplied container.
 *
 * @param container the container to restrict change detection to
 * @param styleName the style name to check
 * @param listener the listener to invoke (function(oldValue, newValue))
 */
function addSelectionStyleChangeListener(container, styleName, listener) {
    var previousStyle = null;
    var checkStyle = function() {
        var currentStyle = selectionStyle(styleName);           // Get the current style on the selection
        if (currentStyle != previousStyle) {
            listener(previousStyle, currentStyle);
            previousStyle = currentStyle;
        }
    };
    container.addEventListener('mouseup', checkStyle);
    container.addEventListener('keyup', checkStyle);
    container.addEventListener('styleChanged', checkStyle);
}