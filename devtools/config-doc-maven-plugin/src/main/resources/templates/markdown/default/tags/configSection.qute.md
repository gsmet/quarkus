{configSection.formatTitle}

| Configuration property | Type | Default |
|------------------------|------|---------|
{#for item in configSection.items}
{#if !item.deprecated}
{#if !item.isSection}
{#configProperty configProperty=item extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{#else}
{#configSection configSection=item extension=extension additionalAnchorPrefix=additionalAnchorPrefix /}
{/if}
{/if}
{/for}