const fs = require('fs');

const inputPath = 'C:\\Users\\thaku\\.gemini\\antigravity\\brain\\4a4731ba-2a69-4e2b-a909-c893a6c2531f\\.system_generated\\steps\\3542\\content.md';
const outputPath = 'c:\\Users\\thaku\\IdeaProjects\\KingdomCore\\webstore\\src\\lib\\materials.ts';

try {
    const rawContent = fs.readFileSync(inputPath, 'utf8');
    
    // Extract JSON part (after the triple dashes)
    const jsonPart = rawContent.split('---')[1].trim();
    const items = JSON.parse(jsonPart);

    const materials = items.map(item => ({
        id: item.name.toUpperCase(),
        name: item.displayName
    }));

    // Generate TypeScript
    const fileContent = `export interface MinecraftMaterial {
  id: string;
  name: string;
}

export const MINECRAFT_MATERIALS: MinecraftMaterial[] = ${JSON.stringify(materials, null, 2)};
`;

    fs.writeFileSync(outputPath, fileContent);
    console.log(`Successfully processed ${materials.length} materials.`);
} catch (error) {
    console.error('Error processing materials:', error);
}
