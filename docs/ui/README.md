# Matriz: Wireframes

Low-fidelity wireframes of the MVP, drawn as SVG (editable text) with a PNG rendering of each. They fix **structure and behaviour**, not visual style. They are deliberately grey. The visual style is a separate decision, see [../constraints.md](../constraints.md), section 4.1.

| Screen | Source | Image | Stories |
|---|---|---|---|
| Dashboard (the project list) | [dashboard-wireframe.svg](dashboard-wireframe.svg) | [wireframe.png](wireframe.png) | US-02 (create), US-06, US-09 (search) |
| Project detail | [project-detail-wireframe.svg](project-detail-wireframe.svg) | [project-detail-wireframe.png](project-detail-wireframe.png) | US-02 (edit), US-03, US-04, US-05, US-09 (filters), US-10, US-11 |
| Task detail | [task-detail-wireframe.svg](task-detail-wireframe.svg) | [task-detail-wireframe.png](task-detail-wireframe.png) | US-05 (edit, delete), US-07, US-08 |

Each wireframe has numbered parts, a legend that ties each part to its requirement and API call, and the main alternative states (loading, empty, error, validation, confirmation). All names and numbers are sample data.

## Not drawn on purpose

These are left to the implementation stage (Ficha 03). Their behaviour is already fixed by the acceptance criteria in [../user-stories.md](../user-stories.md) and by [../../openapi.yaml](../../openapi.yaml):

- The **login** screen: a standard email and password form (AC-01.1, AC-01.2, AC-01.5, AC-01.6).
- The **create project** and **edit project** forms (US-02).
- The **edit task** form (US-05).
- The **manual progress** input, its clear action and its validation message (US-10).

## Rendering the PNG files

Open an SVG in a browser to view it. The PNG files were produced by rendering the SVG in a headless browser at the size given in the SVG's `width` and `height`.
