🔒 Configuration property fixed at build time - All other configuration properties are overridable at runtime

| Configuration property | Type | Default |
|------------------------|------|---------|
{#for item in configItemCollection.items}
{#if !item.deprecated}
{#if item.isSection}
{#configSection configSection=item extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{#else}{#configProperty configProperty=item extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}{/if}
{/if}
{/for}

{#if includeDurationNote}
{#durationNote summaryTableId /}
{/if}
{#if includeMemorySizeNote}
{#memorySizeNote summaryTableId /}
{/if}
