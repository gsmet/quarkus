| <a name="{configSection.toAnchor(extension, additionalAnchorPrefix)}"></a>[**{configSection.formatTitle.escapeCellContent}**](#{configSection.toAnchor(extension, additionalAnchorPrefix)}) | | |
{#for item in configSection.items}
{#if !item.deprecated}
{#if item.isSection}
{#configSection configSection=item extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{#else}
{#configProperty configProperty=item extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{/if}
{/if}
{/for}